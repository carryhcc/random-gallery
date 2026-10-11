package com.example.randomgallery.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.randomgallery.android.data.local.AppSettings
import com.example.randomgallery.android.ui.AppNavHost
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import javax.inject.Inject

/**
 * 单 Activity 宿主。整个 App 的页面切换由 Compose Navigation（[AppNavHost]）驱动，
 * 不再使用 Fragment / nav_graph.xml / BottomNavigationView。
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()   // Android 15+ 强制全面屏，提前主动适配
        super.onCreate(savedInstanceState)
        setContent {
            val darkMode by settings.darkModeFlow.collectAsStateWithLifecycle(initialValue = "system")
            RandomGalleryTheme(darkMode = darkMode) {
                AppNavHost(settings)
            }
        }
    }
}
