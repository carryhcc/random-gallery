package com.example.randomgallery.android.util

import com.example.randomgallery.android.config.BaseUrlConfig

object ImageUrlResolver {

    // 统一清洗媒体链接，避免后端返回反引号或空格导致加载失败
    fun normalize(url: String?): String {
        val cleaned = (url ?: "").trim().trim('`')
        if (cleaned.startsWith("//")) return "https:$cleaned"
        return cleaned
    }

    fun absoluteUrl(url: String?): String {
        val cleaned = normalize(url)
        if (cleaned.startsWith("/") && !cleaned.startsWith("//")) {
            val base = BaseUrlConfig.current().removeSuffix("/")
            return "$base/${cleaned.removePrefix("/")}"
        }
        return cleaned
    }

    // minSdk 28 起 Coil 自动使用 ImageDecoder，原生支持 HEIC/HEIF，无需后端代理
    fun displayUrl(url: String?) = absoluteUrl(url)

    fun rawUrl(url: String?) = absoluteUrl(url)
}
