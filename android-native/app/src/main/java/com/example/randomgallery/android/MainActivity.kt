package com.example.randomgallery.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.ui.AppNavHost
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme
import kotlinx.coroutines.launch

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 单 Activity 宿主。整个 App 的页面切换由 Compose Navigation（[AppNavHost]）驱动，
 * 不再使用 Fragment / nav_graph.xml / BottomNavigationView。
 */
class MainActivity : AppCompatActivity() {

    // 内存缓存标志位：DataStore 只在 onCreate 订阅一次（变化时更新）
    @Volatile
    private var autoReadClipboard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()   // Android 15+ 强制全面屏，提前主动适配
        super.onCreate(savedInstanceState)
        val appPrefs = AppPrefs(this)
        setContent {
            val darkMode by appPrefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = "system")
            RandomGalleryTheme(darkMode = darkMode) {
                AppNavHost()
            }
        }
        lifecycleScope.launch {
            AppPrefs(this@MainActivity).autoReadClipboardFlow.collect { autoReadClipboard = it }
        }
    }
}
