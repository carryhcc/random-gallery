package com.example.randomgallery.android.data.network

import android.content.Context
import com.example.randomgallery.android.config.NodeFailoverInterceptor
import com.example.randomgallery.android.config.NodeFailoverStrategy
import com.example.randomgallery.android.config.CleartextPolicy
import com.example.randomgallery.android.BuildConfig
import com.example.randomgallery.android.data.local.AppSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 网络层装配。
 *
 * 迁移前这里自己维护一个静态 `okHttpClient` 单例，并用 `resetOkHttpClient()` 在切节点 / 换代理时
 * 把它置空重建。客户端的生命周期由 Hilt 单例管理后，这些重置入口改由 [ProxyController] 与
 * [com.example.randomgallery.android.di.BaseUrlManager] 驱动：前者只需清连接池（代理选择器
 * 每次建连都读最新值，不必重建客户端），后者本就无需重建客户端（baseUrl 固化在 Retrofit 上）。
 */
object NetworkModule {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /** 每次换 baseUrl 新建一个轻量 Retrofit 包装；共享客户端避免多个 DiskLruCache 争用同一目录。 */
    fun createApiService(client: OkHttpClient, baseUrl: String): ApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(ApiService::class.java)

    /** 供 Hilt 提供应用级单例客户端（`@Provides` 中装配全部拦截器）。 */
    fun createOkHttpClient(
        context: Context,
        enableHttpLogging: Boolean,
        baseUrlManager: NodeFailoverStrategy,
    ): OkHttpClient {
        val appContext = context.applicationContext
        val clientHeaderInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Client-Native", "android")
                    .build()
            )
        }

        return OkHttpClient.Builder()
            .cache(Cache(File(appContext.cacheDir, "http_cache"), 20L * 1024L * 1024L))
            // 连接超时收紧到 8s（读/写保持 20s）：节点失联时尽快触发故障切换，
            // 避免黑洞地址让首个请求干等 20s 才开始切换
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(clientHeaderInterceptor)
            .addInterceptor(CleartextPolicyInterceptor())
            // 节点故障切换：当前节点连接失败时自动改写到首个存活节点
            .addInterceptor(NodeFailoverInterceptor(baseUrlManager))
            // 离线兜底：网络失败时改用 only-if-cached 重放，命中磁盘缓存返回陈旧数据
            .addInterceptor(OfflineFallbackInterceptor())
            .addNetworkInterceptor(CacheControlInterceptor())
            .addInterceptor(Interceptor { chain ->
                // Cookie 位于 JSON 请求体，debug 日志也不得输出这些请求的正文。
                val sensitive = chain.request().url.encodedPath.let {
                    it.endsWith("/api/xhsWork/download") || it.endsWith("/download/validateCookie")
                }
                HttpLoggingInterceptor().apply {
                    level = if (!enableHttpLogging || sensitive) HttpLoggingInterceptor.Level.NONE
                    else HttpLoggingInterceptor.Level.BODY
                }.intercept(chain)
            })
            .proxySelector(dynamicProxySelector())
            .build()
    }

    /** 动态代理选择器：每次建连时读取最新 [ProxyStore.proxy]，支持运行时切换代理。 */
    fun dynamicProxySelector(): ProxySelector = DynamicProxySelector()
}

/** Enforce the Release cleartext boundary for every request handled by an app-owned OkHttp client. */
class CleartextPolicyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val url = chain.request().url
        if (!CleartextPolicy.permitsHttpUrl(url.toString(), BuildConfig.PERMIT_PUBLIC_CLEARTEXT)) {
            throw IOException("Cleartext HTTP is not permitted for ${url.host}")
        }
        return chain.proceed(chain.request())
    }
}

/**
 * 自定义代理的进程内缓存。
 *
 * 以静态可变状态存在是有意的：OkHttp 的 [ProxySelector] 是长时间存活的连接期回调，
 * 只拿得到 URI，取不到注入的依赖。配置本身由 [ProxyController] 在 IO 线程写入。
 */
internal object ProxyStore {
    @Volatile
    var proxy: Proxy? = null

    suspend fun compute(prefs: AppSettings): Proxy? = runCatching {
        if (!prefs.proxyEnabledFlow.first()) return@runCatching null
        val host = prefs.proxyHostFlow.first()
        val port = prefs.proxyPortFlow.first()
        val typeStr = prefs.proxyTypeFlow.first()
        if (host.isNotBlank() && port in 1..65535) {
            val type = if (typeStr.equals("SOCKS", ignoreCase = true)) Proxy.Type.SOCKS else Proxy.Type.HTTP
            Proxy(type, InetSocketAddress(host, port))
        } else null
    }.getOrNull()
}

/**
 * 可缓存接口判定（白名单）：仅静态元数据接口可被 HTTP 磁盘缓存，
 * 动态接口（随机图、随机套图、环境切换等）一律 no-cache。
 */
private fun isCacheableUrl(url: String): Boolean {
    // 静态元数据：作者列表、标签列表（作品详情含可变时效链接与删除操作，不进行 HTTP 强缓存）
    return url.contains("/api/xhsWork/authors") ||
           url.contains("/api/xhsWork/tags")
}

/**
 * 网络拦截器：对白名单接口自动注入 Cache-Control: max-age=600（10 分钟），
 * 其它接口强制 no-cache / no-store，避免动态接口返回陈旧数据。
 */
private class CacheControlInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val url = chain.request().url.toString()
        return if (isCacheableUrl(url) && response.isSuccessful) {
            response.newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=600")
                .build()
        } else {
            response.newBuilder()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .build()
        }
    }
}

/**
 * 离线兜底拦截器：当网络请求失败（无网/超时/连接被拒绝）时，
 * 自动使用 only-if-cached + max-stale 重试一次，命中磁盘缓存即返回陈旧数据。
 */
private class OfflineFallbackInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (request.method == "GET" && isCacheableUrl(request.url.toString())) {
                val fallbackRequest = request.newBuilder()
                    .cacheControl(
                        CacheControl.Builder()
                            .onlyIfCached()
                            .maxStale(7, TimeUnit.DAYS)
                            .build()
                    )
                    .build()
                runCatching { chain.proceed(fallbackRequest) }.getOrNull()
                    ?.takeIf { it.isSuccessful }
                    ?: throw e
            } else {
                throw e
            }
        }
    }
}

/**
 * 动态代理选择器：每次新建连接时读取最新的 [ProxyStore.proxy]。
 * 这样修改代理设置后无需重建 OkHttpClient 即可对新连接生效（API 与 Coil 图片加载均适用）。
 */
private class DynamicProxySelector : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        val proxy = ProxyStore.proxy
        return if (proxy != null) listOf(proxy) else listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        // 交由 OkHttp 默认行为处理，这里无需干预
    }
}
