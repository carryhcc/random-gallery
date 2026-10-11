package com.example.randomgallery.android.data.network

import com.example.randomgallery.android.data.local.AppSettings
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 代理配置变更后的生效入口。抽成接口以便首页在纯 JVM 测试中替换。
 */
interface ProxySettings {
    /** 重新读取代理配置并让其对新连接生效。 */
    suspend fun refresh()
}

/**
 * 自定义网络代理的读取与生效。
 *
 * 迁移前这段逻辑散在 `NetworkModule` 里：`okHttpClient(context, logging)` 顺带缓存
 * Application Context 并异步预读代理，`HomeViewModel` 改完配置后手动调
 * `NetworkModule.refreshProxy(context)`。副作用藏在"取客户端"这个动作里，且 Anyone 都能触发。
 * 现在抽成显式入口：[prime] 启动时预读一次，[refresh] 在用户改配置后重新读取。
 *
 * 生效机制不变：OkHttp 的 [okhttp3.ProxySelector] 每次建连都会回调，
 * 因此只需更新 [ProxyStore.proxy]，无需重建客户端；再清一次连接池，
 * 让已经建立的长连接不会继续走旧代理。
 */
@Singleton
class ProxyController @Inject constructor(
    private val prefs: AppSettings,
    private val okHttpClient: Lazy<OkHttpClient>,
) : ProxySettings {

    private val primed = AtomicBoolean(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 启动时预读一次。在 IO 线程读 DataStore 并缓存，避免首个请求在主线程同步阻塞；
     * 预读完成前 [ProxyStore.proxy] 为 null，等价直连。
     */
    fun prime() {
        if (!primed.compareAndSet(false, true)) return
        scope.launch { ProxyStore.proxy = ProxyStore.compute(prefs) }
    }

    /** 用户修改代理配置后重新读取并应用。 */
    override suspend fun refresh() {
        ProxyStore.proxy = ProxyStore.compute(prefs)
        // 旧连接可能仍绑定着上一个代理，清掉连接池让后续请求重新建连
        withContext(Dispatchers.IO) {
            runCatching { okHttpClient.get().connectionPool.evictAll() }
        }
    }
}
