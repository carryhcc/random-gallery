package com.example.randomgallery.android.config

import com.example.randomgallery.android.BuildConfig
import java.net.URI

object BaseUrlConfig {

    @Volatile
    private var currentBaseUrl: String = BuildConfig.DEFAULT_BASE_URL

    fun sanitize(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.query != null || uri.fragment != null) return null

        return uri.toString().let { if (it.endsWith("/")) it else "$it/" }
    }

    fun resolve(custom: String?, fallback: String): String {
        return sanitize(custom) ?: sanitize(fallback) ?: BuildConfig.DEFAULT_BASE_URL
    }

    fun current(): String = currentBaseUrl

    fun update(baseUrl: String) {
        currentBaseUrl = resolve(baseUrl, BuildConfig.DEFAULT_BASE_URL)
    }
}
