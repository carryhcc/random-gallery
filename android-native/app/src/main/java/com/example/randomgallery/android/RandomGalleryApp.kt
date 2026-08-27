package com.example.randomgallery.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.size.Precision
import com.example.randomgallery.android.util.HeifSystemFirstDecoder
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class RandomGalleryApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        val imageDispatcher = Dispatcher().apply {
            maxRequests = 10
            maxRequestsPerHost = 4
        }

        val okHttpClientBuilder = OkHttpClient.Builder()
            .dispatcher(imageDispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(CoilNetworkInterceptor())

        com.example.randomgallery.android.data.network.NetworkModule.buildCustomProxy(this)?.let { proxy ->
            okHttpClientBuilder.proxy(proxy)
        }

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClientBuilder.build())
            .precision(Precision.INEXACT) // 启用按 View 实际尺寸下采样/缩放，防止大图原尺寸加载
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // 提升磁盘缓存配额
                    .build()
            }
            .components {
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                // HEIC/HEIF/AVIF：优先系统解码，失败自动降级 libheif 软解（见 HeifSystemFirstDecoder）
                add(HeifSystemFirstDecoder.Factory())
            }
            .build()
    }

    private class CoilNetworkInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url

            // 1. 如果是直连小红书 CDN，自动加上 Referer 和 User-Agent
            val builder = request.newBuilder()
            val host = url.host
            if (host.contains("xhscdn.com") || host.contains("xiaohongshu.com")) {
                builder.header("Referer", "https://www.xiaohongshu.com/")
                builder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SD1A.210817.036; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                builder.header(
                    "Accept",
                    "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
                )
            }

            val targetRequest = builder.build()
            var response: Response? = null
            var exception: IOException? = null

            try {
                response = chain.proceed(targetRequest)
            } catch (e: IOException) {
                exception = e
            }

            if (response != null) {
                // 3. 规避 Spring Boot force UTF-8 编码导致 content-type 变成 "image/jpeg;charset=UTF-8" 的问题
                val contentType = response.header("Content-Type")
                if (contentType != null && contentType.startsWith("image/jpeg;charset=UTF-8", ignoreCase = true)) {
                    return response.newBuilder()
                        .header("Content-Type", "image/jpeg")
                        .build()
                }
                return response
            }

            throw exception ?: IOException("Network request failed")
        }
    }
}
