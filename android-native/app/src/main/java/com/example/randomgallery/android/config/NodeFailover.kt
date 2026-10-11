package com.example.randomgallery.android.config

import java.io.IOException

/**
 * 节点故障切换所依赖的最小能力集。
 *
 * 抽成接口是为了让 [NodeFailoverInterceptor] 不直接依赖 `BaseUrlManager`
 * （后者建仓库、建仓库要 OkHttpClient、建客户端又要传本策略，双向依赖会成环）。
 */
interface NodeFailoverStrategy {

    /** 当前生效的服务地址。 */
    fun currentBaseUrl(): String

    /**
     * 探测并返回一个可用的备用节点；排除当前节点，本地节点优先，全部失联返回 null。
     * 会发起真实网络请求，仅可在 IO 线程调用。
     */
    suspend fun pickFailoverTarget(): String?

    /** 后台完成应用级切换（持久化 + 重建仓库）；不阻塞、不抛异常。 */
    fun switchToInBackground(baseUrl: String)
}

/** 失败改写标记：标识由故障切换改写过的请求，防止再次触发切换。 */
private const val FAILOVER_HEADER = "X-Failover-Retry"

/**
 * 运行期节点故障切换拦截器：当前节点连接失败（IOException）时，
 * 对无标记的 GET 请求探测其它节点（本地优先），改写 URL 到首个存活节点重试；
 * 成功后再由 [NodeFailoverStrategy.switchToInBackground] 完成应用级切换。
 *
 * POST（下载/环境切换等）与已改写的请求不做切换，直接抛出原异常。
 */
class NodeFailoverInterceptor(
    private val strategy: NodeFailoverStrategy,
) : okhttp3.Interceptor {

    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (request.method != "GET" || request.header(FAILOVER_HEADER) != null) throw e

            // 乐观重试：先用改写后的 URL 拿到响应，切换应用级地址放到后台，
            // 避免探测 + 持久化的耗时刻意拖慢这一次用户可见的请求。
            val picked = kotlinx.coroutines.runBlocking { strategy.pickFailoverTarget() } ?: throw e

            val retry = request.newBuilder()
                .url(rewrite(request, picked))
                .header(FAILOVER_HEADER, "1")
                .build()
            val response = chain.proceed(retry)

            strategy.switchToInBackground(picked)
            response
        }
    }

    /** 同路径换源：保留原请求的 path 与 query，仅替换 scheme+authority。 */
    private fun rewrite(request: okhttp3.Request, newBase: String): String {
        val query = request.url.encodedQuery
        return newBase.removeSuffix("/") + request.url.encodedPath + (query?.let { "?$it" } ?: "")
    }
}
