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
 * 响应缓存头注入：GET 响应没有 Cache-Control 时补上短 TTL，
 * 让 20MB 磁盘缓存真正生效（列表等只读接口可复用/离线读取）。
 */
private class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val request = chain.request()
        // 随机图/GIF 接口不注入缓存头：随机内容不应被 HTTP 磁盘缓存复用，
        // 过期/离线兜底统一交给仓库层 24h TTL 缓存判断
        val isRandomEndpoint = request.url.encodedPath.contains("random", ignoreCase = true)
        if (request.method == "GET" && !isRandomEndpoint && response.header("Cache-Control") == null) {
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
 * 命中磁盘缓存即返回陈旧数据；否则保持原异常抛出。
 */
private class OfflineFallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
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
