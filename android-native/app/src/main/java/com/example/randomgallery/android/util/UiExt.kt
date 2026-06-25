package com.example.randomgallery.android.util

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar

fun View.shortSnack(text: String) {
    Snackbar.make(this, text, Snackbar.LENGTH_SHORT).show()
}

fun Activity.showTopMessage(msg: String, durationMs: Long = 3000) {
    val root = window.decorView as ViewGroup
    val density = resources.displayMetrics.density
    val hPad = (16 * density).toInt()
    val vPad = (10 * density).toInt()
    val radius = (12 * density)
    val topMargin = (72 * density).toInt()

    val bg = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.parseColor("#CC1C1C1E"))
        cornerRadius = radius
    }

    val tv = TextView(this).apply {
        text = msg
        setTextColor(Color.WHITE)
        textSize = 14f
        setPadding(hPad, vPad, hPad, vPad)
        background = bg
        maxLines = 3
    }

    val lp = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        topMargin = topMargin
    }

    tv.alpha = 0f
    tv.translationY = -(24 * density)
    root.addView(tv, lp)
    tv.animate().alpha(1f).translationY(0f).setDuration(200).start()

    tv.postDelayed({
        tv.animate().alpha(0f).translationY(-(24 * density)).setDuration(200)
            .withEndAction { root.removeView(tv) }.start()
    }, durationMs)
}

fun Context.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Context.downloadToPublic(url: String, fileName: String) {
    val manager = getSystemService<DownloadManager>() ?: return
    val request = DownloadManager.Request(url.toUri())
        .setTitle(fileName)
        .setDescription("正在下载")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    if (url.contains("xhscdn.com") || url.contains("xiaohongshu.com")) {
        request.addRequestHeader("Referer", "https://www.xiaohongshu.com/")
        request.addRequestHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }
    manager.enqueue(request)
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

fun View.applySystemBarsPadding(
    top: Boolean = true,
    bottom: Boolean = true,
    left: Boolean = false,
    right: Boolean = false
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            left = initialLeft + if (left) bars.left else 0,
            top = initialTop + if (top) bars.top else 0,
            right = initialRight + if (right) bars.right else 0,
            bottom = initialBottom + if (bottom) bars.bottom else 0
        )
        insets
    }
}
