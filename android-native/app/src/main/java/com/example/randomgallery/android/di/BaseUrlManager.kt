package com.example.randomgallery.android.di

import com.example.randomgallery.android.BuildConfig
import com.example.randomgallery.android.config.ApiNodeSwitcher
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.config.NodeFailoverStrategy
import com.example.randomgallery.android.data.local.AppSettings
import com.example.randomgallery.android.data.local.CachedPayloadDao
import com.example.randomgallery.android.data.network.NetworkModule
import com.example.randomgallery.android.data.repository.GalleryRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务地址（API 节点）的运行时管理者。
 *
 * 迁移前这是 `AppContainer` 里的一堆静态字段：`repository` 与 `repositoryBaseUrl` 两个
 * `@Volatile` 变量加双重检查锁，任何人拿到 Context 就能改全局地址。现在收敛成一个单例，
 * 写入路径只有 [updateBaseUrl] 一条，且与仓库重建在同一临界区内完成。
 *
 * ## 为什么仓库要重建而不是常驻
 * [com.example.randomgallery.android.data.network.ApiService] 全部是相对路径
 * （如 `@GET("api/pic/random/one")`），baseUrl 在建 Retrofit 时就固化进实例了；
 * 换节点必须换一个 Retrofit，也就必须换一个 [GalleryRepository]。
 * 仓库内部另有以 baseUrl 为 key 的 TTL 缓存，重建同时让这些缓存自然失效。
 *
 * ## 与节点故障切换的关系
 * [com.example.randomgallery.android.config.NodeFailoverInterceptor] 在请求失败时先就地
 * 改写这一次请求的 URL 重试，成功后再调 [switchToInBackground] 完成应用级切换。于是本类要建仓库、
 * 建仓库要 OkHttpClient、建 OkHttpClient 又要传本类 —— 为断开这个环，客户端以 [Lazy]
 * 注入：环在编译期是单向的，运行期首次取用时客户端早已装配完成。
 */
@Singleton
class BaseUrlManager @Inject constructor(
    private val prefs: AppSettings,
    private val cacheDao: CachedPayloadDao,
    private val okHttpClient: Lazy<OkHttpClient>,
) : NodeManager, NodeFailoverStrategy {

    private val cacheLock = Any()

    private val initScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }
    )

    @Volatile
    private var cachedRepository: GalleryRepository? = null

    @Volatile
    private var cachedBaseUrl: String? = null

    /** 进程内串行化地址变更，避免并发切换时两次重建互相覆盖。 */
    private val switchMutex = Mutex()

    /** 当前生效的服务地址。未读过 DataStore 前即为构建期默认值。 */
    override fun currentBaseUrl(): String = BaseUrlConfig.current()

    /**
     * 取当前地址对应的仓库，地址变了自动重建。
     *
     * 保持「每次调用都取一次」的语义，而不是让 ViewModel 在构造期持有仓库实例：
     * 换节点后旧 ViewModel 仍活在返回栈里，下一次取用时必须拿到指向新节点的仓库。
     */
    override fun repository(): GalleryRepository {
        val baseUrl = BaseUrlConfig.current()
        cachedRepository?.let { if (cachedBaseUrl == baseUrl) return it }

        synchronized(cacheLock) {
            cachedRepository?.let { if (cachedBaseUrl == baseUrl) return it }
            return GalleryRepository(
                api = NetworkModule.createApiService(
                    client = okHttpClient.get(),
                    baseUrl = baseUrl,
                ),
                cacheDao = cacheDao,
                prefs = prefs,
            ).also {
                cachedRepository = it
                cachedBaseUrl = baseUrl
            }
        }
    }

    /** 丢弃当前仓库。代理配置变更等需要重建底层连接的场景使用。 */
    override fun invalidateRepository() {
        synchronized(cacheLock) {
            cachedRepository = null
            cachedBaseUrl = null
        }
    }

    /**
     * 切换到新的服务地址：持久化 → 更新当前地址 → 丢弃旧仓库。
     *
     * 三步必须在同一临界区内完成，否则并发切换会出现「地址已换、UI 高亮的却是旧值」，
     * 或新旧仓库交叉写盘。
     */
    override suspend fun updateBaseUrl(rawUrl: String) = switchMutex.withLock {
        val resolved = BaseUrlConfig.resolve(rawUrl, BuildConfig.DEFAULT_BASE_URL)
        withContext(Dispatchers.IO) { prefs.saveBaseUrl(resolved) }
        BaseUrlConfig.update(resolved)
        synchronized(cacheLock) {
            cachedRepository = null
            cachedBaseUrl = null
        }
    }

    /**
     * 启动初始化：先用构建期默认地址立即可用（不阻塞主线程），
     * 随后异步读 DataStore 并探测候选节点。
     */
    fun initialize() {
        BaseUrlConfig.update(BaseUrlConfig.resolve(null, BuildConfig.DEFAULT_BASE_URL))
        initScope.launch {
            val resolved = BaseUrlConfig.resolve(prefs.getBaseUrl(), BuildConfig.DEFAULT_BASE_URL)
            if (resolved != BaseUrlConfig.current()) {
                BaseUrlConfig.update(resolved)
                invalidateRepository()
            }
            // 启动自动选节点：本地节点优先 ping，首个存活节点即用；全部失联则保持现状
            runCatching {
                val picked = ApiNodeSwitcher.autoSelect(ApiNodeSwitcher.candidateUrls(prefs))
                if (picked != null && picked != BaseUrlConfig.current()) {
                    updateBaseUrl(picked)
                    ApiNodeSwitcher.emit("已自动切换节点：$picked")
                }
            }
        }
    }

    // ── NodeFailoverStrategy ────────────────────────────────────────────

    override suspend fun pickFailoverTarget(): String? {
        val current = BaseUrlConfig.current()
        return ApiNodeSwitcher.autoSelect(
            candidates = ApiNodeSwitcher.candidateUrls(prefs).filter { it != current },
            exclude = current,
        )
    }

    /**
     * 拦截器已用改写后的请求拿到响应，这里在后台完成应用级切换。
     * 调用方已经拿到响应，因此不能再抛出异常影响它。
     */
    override fun switchToInBackground(baseUrl: String) {
        initScope.launch {
            updateBaseUrl(baseUrl)
            ApiNodeSwitcher.emit("节点失联，已自动切换到 $baseUrl")
        }
    }
}
