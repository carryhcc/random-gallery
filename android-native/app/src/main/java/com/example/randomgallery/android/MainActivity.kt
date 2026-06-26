package com.example.randomgallery.android

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.ui.AppNavHost
import com.example.randomgallery.android.ui.download.DownloadManageViewModel
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme
import com.example.randomgallery.android.util.showTopMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 单 Activity 宿主。整个 App 的页面切换由 Compose Navigation（[AppNavHost]）驱动，
 * 不再使用 Fragment / nav_graph.xml / BottomNavigationView。
 */
class MainActivity : AppCompatActivity() {

    private var lastAutoSubmittedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()   // Android 15+ 强制全面屏，提前主动适配
        super.onCreate(savedInstanceState)
        setContent {
            RandomGalleryTheme {
                AppNavHost()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        lifecycleScope.launch {
            if (!AppPrefs(this@MainActivity).autoReadClipboardFlow.first()) return@launch

            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
            val url = DownloadManageViewModel.extractHttpUrl(text) ?: return@launch
            val isXhsLink = url.contains("xhslink.com") || url.contains("xiaohongshu.com")
            if (!isXhsLink) return@launch

            if (url == lastAutoSubmittedUrl) return@launch
            lastAutoSubmittedUrl = url

            val result = AppContainer.repository(this@MainActivity).addDownloadTask(url)
            val msg = if (result.isSuccess) {
                "✓ 已加入下载队列"
            } else {
                val err = result.exceptionOrNull()
                val isNetworkError = err is java.net.UnknownHostException
                    || err is java.net.ConnectException
                    || err is java.net.SocketTimeoutException
                if (isNetworkError) "提交失败：网络连接错误" else "提交失败：${err?.message ?: "未知错误"}"
            }
            showTopMessage(msg)
        }
    }
}
