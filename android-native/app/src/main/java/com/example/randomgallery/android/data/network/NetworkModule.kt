package com.example.randomgallery.android.data.network

import android.content.Context
import com.example.randomgallery.android.data.local.AppPrefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.TimeUnit

object NetworkModule {

    // 全应用共享同一个 OkHttpClient（含磁盘缓存目录）：
    // 切换 baseUrl 只新建轻量 Retrofit 包装，避免多个 DiskLruCache 争用同一目录导致缓存被静默禁用。
    @Volatile
    private var okHttpClient: OkHttpClient? = null

    fun okHttpClient(context: Context, enableHttpLogging: Boolean): OkHttpClient {
        okHttpClient?.let { return it }
        return synchronized(this) {
            okHttpClient ?: createOkHttp(context.applicationContext, enableHttpLogging).also { okHttpClient = it }
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

    fun buildCustomProxy(context: Context): Proxy? {
        val prefs = AppPrefs(context.applicationContext)
        return runCatching {
            runBlocking {
                val enabled = prefs.proxyEnabledFlow.first()
                if (enabled) {
                    val host = prefs.proxyHostFlow.first()
                    val port = prefs.proxyPortFlow.first()
                    val typeStr = prefs.proxyTypeFlow.first()
                    if (host.isNotBlank() && port in 1..65535) {
                        val type = if (typeStr.equals("SOCKS", ignoreCase = true)) Proxy.Type.SOCKS else Proxy.Type.HTTP
                        Proxy(type, InetSocketAddress(host, port))
                    } else null
                } else null
            }
        }.getOrNull()
    }

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

        buildCustomProxy(context)?.let { proxy ->
            builder.proxy(proxy)
        }

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
    // 静态元数据：作者列表、标签列表、下载作品详情
    return url.contains("/api/xhs/work/authors") ||
           url.contains("/api/xhs/work/tags") ||
           url.contains("/api/xhs/work/detail")
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
