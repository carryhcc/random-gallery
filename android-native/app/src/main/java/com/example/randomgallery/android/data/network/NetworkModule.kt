package com.example.randomgallery.android.data.network

import android.content.Context
import com.example.randomgallery.android.data.local.AppPrefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import java.util.concurrent.atomic.AtomicBoolean

// 自定义代理（模块级，供 NetworkModule 内部类与 Coil 图片加载器共享读取）
@Volatile
private var cachedProxy: Proxy? = null

object NetworkModule {

    // 全应用共享同一个 OkHttpClient（含磁盘缓存目录）：
    // 切换 baseUrl 只新建轻量 Retrofit 包装，避免多个 DiskLruCache 争用同一目录导致缓存被静默禁用。
    @Volatile
    private var okHttpClient: OkHttpClient? = null

    // 应用上下文（首次构建客户端时缓存），用于后台预读代理配置
    @Volatile
    private var appContext: Context? = null

    // 自定义代理在 IO 线程预读并缓存，避免在主线程读取时阻塞
    private val proxyPrereadStarted = AtomicBoolean(false)

    fun okHttpClient(context: Context, enableHttpLogging: Boolean): OkHttpClient {
        appContext = context.applicationContext
        ensureProxyPreread()
        okHttpClient?.let { return it }
        return synchronized(this) {
            okHttpClient ?: createOkHttp(context.applicationContext, enableHttpLogging).also { okHttpClient = it }
        }
    }

    private fun ensureProxyPreread() {
        if (proxyPrereadStarted.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                cachedProxy = computeProxy(appContext)
            }
        }
    }

    fun resetOkHttpClient() {
        synchronized(this) {
            okHttpClient = null
        }
    }

    fun clearHttpCache() {
        runCatching { okHttpClient?.cache?.evictAll() }
    }

    /**
     * 代理配置变更后重新读取并应用：更新 [cachedProxy] 后重建底层 OkHttpClient，
     * 配合 [DynamicProxySelector] 使后续新建连接立即走新代理（无需重启进程）。
     */
    suspend fun refreshProxy(context: Context) {
        cachedProxy = computeProxy(context.applicationContext)
        resetOkHttpClient()
    }

    /** 动态代理选择器：每次建连时读取最新 [cachedProxy]，支持运行时切换代理。 */
    fun dynamicProxySelector(): ProxySelector = DynamicProxySelector()

    /**
     * 在 IO 线程读取自定义代理配置并缓存到 [cachedProxy]，避免在调用线程（可能为主线程）
     * 同步阻塞。首次构建 OkHttpClient 时异步预读可能尚未完成
     * （cachedProxy 为 null，等价直连）；当用户修改代理配置时
     * [com.example.randomgallery.android.AppContainer.clearRepository] 会重建客户端，
     * 配合 [dynamicProxySelector] 与 [refreshProxy] 使新连接立即走新代理。
     */
    private suspend fun computeProxy(context: Context?): Proxy? = runCatching {
        val ctx = context ?: return@runCatching null
        val prefs = AppPrefs(ctx)
        val enabled = prefs.proxyEnabledFlow.first()
        if (!enabled) return@runCatching null
        val host = prefs.proxyHostFlow.first()
        val port = prefs.proxyPortFlow.first()
        val typeStr = prefs.proxyTypeFlow.first()
        if (host.isNotBlank() && port in 1..65535) {
            val type = if (typeStr.equals("SOCKS", ignoreCase = true)) Proxy.Type.SOCKS else Proxy.Type.HTTP
            Proxy(type, InetSocketAddress(host, port))
        } else null
    }.getOrNull()

    private fun createOkHttp(context: Context, enableHttpLogging: Boolean): OkHttpClient {
        val cacheDir = context.cacheDir
        val logging = HttpLoggingInterceptor().apply {
            level = if (enableHttpLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val clientHeaderInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Client-Native", "android")
                    .build()
            )
        }

        val builder = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http_cache"), 20L * 1024L * 1024L))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(clientHeaderInterceptor)
            // 离线兜底：网络失败时改用 only-if-cached 重放，命中磁盘缓存返回陈旧数据
            .addInterceptor(OfflineFallbackInterceptor())
            .addNetworkInterceptor(CacheControlInterceptor())
            .addInterceptor(logging)

        builder.proxySelector(dynamicProxySelector())

        return builder.build()
    }

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createApiService(
        context: Context,
        baseUrl: String,
        enableHttpLogging: Boolean
    ): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient(context, enableHttpLogging))
            .build()
        return retrofit.create(ApiService::class.java)
    }
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
 * 动态代理选择器：每次新建连接时读取最新的 [cachedProxy]。
 * 这样修改代理设置后无需重建 OkHttpClient 即可对新连接生效（API 与 Coil 图片加载均适用）。
 */
private class DynamicProxySelector : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        val proxy = cachedProxy
        return if (proxy != null) listOf(proxy) else listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        // 交由 OkHttp 默认行为处理，这里无需干预
    }
}
