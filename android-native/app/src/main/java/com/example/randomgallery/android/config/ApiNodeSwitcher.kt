package com.example.randomgallery.android.config

import com.example.randomgallery.android.BuildConfig
import com.example.randomgallery.android.data.local.AppSettings
import com.example.randomgallery.android.data.network.CleartextPolicyInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 服务端 API 节点探测与自动切换：
 * - "ping" 节点 = 对 `api/system/env/current` 发起短超时 GET，能完成任意 HTTP 交互（含 4xx/5xx）即视为可达；
 * - 本地节点（localhost / 内网 IPv4）优先探测，存活即优先选用；
 * - 启动时由 [com.example.randomgallery.android.di.BaseUrlManager.initialize] 自动选节点；
 * - 运行期当前节点连接失败时，由 [NodeFailoverInterceptor] 改写请求到首个存活节点重试，
 *   成功后在后台完成应用级切换（持久化 + 重建仓库），后续请求直达新节点。
 */
object ApiNodeSwitcher {

    private const val HEALTH_PATH = "api/system/env/current"
    private const val PROBE_TIMEOUT_MS = 2500L

    /** 节点自动切换事件（供 UI 提示）；无订阅者时静默丢弃。 */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(message: String) {
        _events.tryEmit(message)
    }

    // 探测专用客户端：短超时、不重试、无缓存，与业务客户端隔离
    private val probeClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(CleartextPolicyInterceptor())
            .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(PROBE_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * 本地节点判定：localhost / mDNS `.local` / 内网 IPv4。
     *
     * 与 [CleartextPolicy.isPrivateHost] 同一套判定并复用它：「本地优先」的排序依据
     * 与「允许明文」的放行依据必须是同一个概念，各写一份迟早会漂移
     * （例如一边认 169.254 另一边不认），表现为排序与放行结果互相矛盾。
     */
    fun isLocalNode(url: String): Boolean = CleartextPolicy.isPrivateHost(url)

    /** 本地节点优先的稳定排序（两组各自保持原有相对顺序）。 */
    fun orderLocalFirst(candidates: List<String>): List<String> {
        val (local, remote) = candidates.partition { isLocalNode(it) }
        return local + remote
    }

    /** 阻塞式 ping：任意 HTTP 响应都算节点可达。仅限 IO 线程调用。 */
    fun probe(url: String): Boolean = runCatching {
        val healthUrl = url.removeSuffix("/") + "/" + HEALTH_PATH
        probeClient.newCall(
            Request.Builder()
                .url(healthUrl)
                .header("X-Client-Native", "android")
                .get()
                .build()
        ).execute().use { true }
    }.getOrDefault(false)

    /** 候选节点合并：已保存节点 + 节点列表 + 默认节点，逐个清洗后去重（保持首次出现顺序）。 */
    fun mergeCandidates(saved: String?, nodeList: List<String>, default: String): List<String> =
        (listOfNotNull(saved) + nodeList + listOf(default))
            .mapNotNull { BaseUrlConfig.sanitize(it) }
            .distinct()

    /**
     * 候选节点列表。取 [AppSettings] 而非 Context：本对象原先自己 `AppPrefs(context)`
     * 另建一个偏好实例，绕开了依赖图，也让调用方无从替换。
     */
    suspend fun candidateUrls(prefs: AppSettings): List<String> {
        return mergeCandidates(
            saved = prefs.getBaseUrl(),
            nodeList = prefs.getUrlList(),
            default = BuildConfig.DEFAULT_BASE_URL
        )
    }

    /**
     * 依次 ping（本地节点优先），返回首个存活节点；全部失联返回 null。
     * [exclude] 用于运行期故障切换时跳过已失联的当前节点。
     */
    suspend fun autoSelect(candidates: List<String>, exclude: String? = null): String? =
        withContext(Dispatchers.IO) {
            orderLocalFirst(candidates)
                .filter { it != exclude }
                .firstOrNull { probe(it) }
        }
}
