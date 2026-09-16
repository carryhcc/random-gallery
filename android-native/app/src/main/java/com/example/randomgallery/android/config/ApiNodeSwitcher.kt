package com.example.randomgallery.android.config

import android.content.Context
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.BuildConfig
import com.example.randomgallery.android.data.local.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 服务端 API 节点探测与自动切换：
 * - "ping" 节点 = 对 `api/system/env/current` 发起短超时 GET，能完成任意 HTTP 交互（含 4xx/5xx）即视为可达；
 * - 本地节点（localhost / 内网 IPv4）优先探测，存活即优先选用；
 * - 启动时由 [AppContainer.initialize] 自动选节点；
 * - 运行期当前节点连接失败时，由 [ApiNodeFailoverInterceptor] 改写请求到首个存活节点重试，
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
            .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(PROBE_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * 本地节点判定：localhost / mDNS `.local` / 内网 IPv4。
     * 10.x 覆盖 Android 模拟器宿主别名 10.0.2.2。
     */
    fun isLocalNode(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        if (host == "localhost" || host.endsWith(".local")) return true
        val parts = host.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        val a = parts[0]
        val b = parts[1]
        return a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31)
    }

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

    suspend fun candidateUrls(context: Context): List<String> {
        val prefs = AppPrefs(context.applicationContext)
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

/** 失败改写标记：标识由故障切换改写过的请求，防止再次触发切换。 */
private const val FAILOVER_HEADER = "X-Failover-Retry"

/**
 * 运行期节点故障切换拦截器：当前节点连接失败（IOException）时，
 * 对无标记的 GET 请求探测其它节点（本地优先），改写 URL 到首个存活节点重试；
 * 成功后在后台完成应用级切换（持久化 + 重建仓库 + 图片域名跟随）。
 * POST（下载/环境切换等）与已改写的请求不做切换，直接抛出原异常。
 */
class ApiNodeFailoverInterceptor(private val appContext: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (request.method != "GET" || request.header(FAILOVER_HEADER) != null) throw e

            val current = BaseUrlConfig.current()
            val alternates = runBlocking { ApiNodeSwitcher.candidateUrls(appContext) }
                .filter { it != current }
            val picked = runBlocking { ApiNodeSwitcher.autoSelect(alternates, exclude = current) }
                ?: throw e

            val retry = request.newBuilder()
                .url(rewrite(request, picked))
                .header(FAILOVER_HEADER, "1")
                .build()
            val response = chain.proceed(retry)

            // 后台完成应用级切换：updateBaseUrl 会持久化、清缓存并重建仓库，
            // 后续请求直达新节点；UI 高亮经 prefs.baseUrlFlow 自动跟随。
            CoroutineScope(Dispatchers.IO).launch {
                AppContainer.updateBaseUrl(appContext, picked)
                ApiNodeSwitcher.emit("节点失联，已自动切换到 $picked")
            }
            response
        }
    }

    /** 同路径换源：保留原请求的 path 与 query，仅替换 scheme+authority。 */
    private fun rewrite(request: Request, newBase: String): String {
        val query = request.url.encodedQuery
        return newBase.removeSuffix("/") + request.url.encodedPath + (query?.let { "?$it" } ?: "")
    }
}
