package com.example.randomgallery.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

class RandomGalleryApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(CoilNetworkInterceptor())
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .components {
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
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
                builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
