package com.example.randomgallery.android.data.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
import java.util.concurrent.TimeUnit

object NetworkModule {

    // 全应用共享同一个 OkHttpClient（含磁盘缓存目录）：
    // 切换 baseUrl 只新建轻量 Retrofit 包装，避免多个 DiskLruCache 争用同一目录导致缓存被静默禁用。
    @Volatile
    private var okHttpClient: OkHttpClient? = null

    fun okHttpClient(context: Context, enableHttpLogging: Boolean): OkHttpClient {
        okHttpClient?.let { return it }
        return synchronized(this) {
            okHttpClient ?: createOkHttp(context.applicationContext.cacheDir, enableHttpLogging).also { okHttpClient = it }
        }
    }

    fun clearHttpCache() {
        runCatching { okHttpClient?.cache?.evictAll() }
    }

    private fun createOkHttp(cacheDir: File, enableHttpLogging: Boolean): OkHttpClient {
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

        return OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http_cache"), 20L * 1024L * 1024L))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(clientHeaderInterceptor)
            // 离线兜底：网络失败时改用 only-if-cached 重放，命中磁盘缓存返回陈旧数据
            .addInterceptor(OfflineFallbackInterceptor())
            .addNetworkInterceptor(CacheControlInterceptor())
            .addInterceptor(logging)
            .build()
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
 * 其余动态接口一律不缓存、不参与离线兜底，保证数据实时。
 */
private fun isCacheablePath(path: String): Boolean =
    path.contains("xhsWork/authors", ignoreCase = true) ||
        path.contains("xhsWork/tags", ignoreCase = true)

/**
 * 响应缓存头注入：对白名单接口的 GET 响应补上短 TTL，
 * 让 20MB 磁盘缓存生效（作者/标签等静态元数据可复用/离线读取）。
 */
private class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val request = chain.request()
        if (request.method == "GET" && isCacheablePath(request.url.encodedPath) && response.header("Cache-Control") == null) {
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=${CACHE_MAX_AGE_SECONDS}")
                .build()
        }
        return response
    }

    private companion object {
        const val CACHE_MAX_AGE_SECONDS = 300L
    }
}

/**
 * 离线兜底：首次请求因无网络抛 IOException 时，用 only-if-cached + max-stale 重放，
 * 命中磁盘缓存即返回陈旧数据；否则保持原异常抛出。仅对白名单接口生效。
 */
private class OfflineFallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isCacheablePath(request.url.encodedPath)) return chain.proceed(request)
        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            if (request.method != "GET") throw e
            val cachedRequest = request.newBuilder()
                .cacheControl(
                    CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(7, TimeUnit.DAYS)
                        .build()
                )
                .build()
            return chain.proceed(cachedRequest)
        }
    }
}
