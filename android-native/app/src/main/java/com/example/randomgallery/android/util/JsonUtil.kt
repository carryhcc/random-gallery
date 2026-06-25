package com.example.randomgallery.android.util

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtil {

    @PublishedApi
    internal val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    inline fun <reified T> toJson(data: T): String {
        return moshi.adapter(T::class.java).toJson(data)
    }

    inline fun <reified T> fromJson(json: String): T? {
        return try {
            moshi.adapter(T::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        }
    }
}
