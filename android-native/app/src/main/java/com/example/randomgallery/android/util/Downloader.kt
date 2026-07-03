package com.example.randomgallery.android.util

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.content.getSystemService
import androidx.core.net.toUri

enum class MediaKind { IMAGE, VIDEO }

/** 全 App 唯一的系统下载入口：统一文件命名、目录、XHS 防盗链头。 */
object Downloader {

    fun enqueue(context: Context, url: String, kind: MediaKind, subDir: String = "RandomGallery") {
        val manager = context.getSystemService<DownloadManager>() ?: return
        val defaultExt = if (kind == MediaKind.VIDEO) "mp4" else "jpg"
        val fileName = buildDownloadFileName(url, "rg", defaultExt)
        val dir = if (kind == MediaKind.VIDEO) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES

        val request = DownloadManager.Request(url.toUri())
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(dir, "$subDir/$fileName")
            .setAllowedOverMetered(true)

        if (url.contains("xhscdn.com") || url.contains("xiaohongshu.com")) {
            request.addRequestHeader("Referer", "https://www.xiaohongshu.com/")
            request.addRequestHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
        }
        manager.enqueue(request)
    }
}

fun buildDownloadFileName(url: String, prefix: String, defaultExt: String): String {
    val cleanExt = defaultExt.removePrefix(".")
    val lastSegment = url.substringBefore("?").substringAfterLast("/").trim()
    if (lastSegment.isBlank()) {
        return "${prefix}_${System.currentTimeMillis()}.$cleanExt"
    }
    return if (lastSegment.substringAfterLast(".", "").isNotBlank()) {
        lastSegment
    } else {
        "$lastSegment.$cleanExt"
    }
}
