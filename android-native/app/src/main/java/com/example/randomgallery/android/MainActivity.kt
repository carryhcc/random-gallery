package com.example.randomgallery.android

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.randomgallery.android.databinding.ActivityMainBinding
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.ui.download.DownloadManageViewModel
import com.example.randomgallery.android.util.applySystemBarsPadding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var backPressedAt = 0L
    private var isSyncingNav = false
    private var lastAutoSubmittedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        enableEdgeToEdge()   // Android 15+ 强制全面屏，提前主动适配
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        val topLevelIds = setOf(
            R.id.homeFragment,
            R.id.randomGalleryFragment,
            R.id.groupListFragment,
            R.id.downloadListFragment
        )
        // Pages where bottom nav should stay visible (tabs + secondary pages)
        val navVisibleIds = topLevelIds + setOf(
            R.id.picListFragment
        )

        binding.navHost.applySystemBarsPadding(top = false, bottom = false, left = false, right = false)
        binding.bottomNav.applySystemBarsPadding(top = false, bottom = true, left = false, right = false)

        // Custom bottom nav listener — avoids setupWithNavController state-sync bugs
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (isSyncingNav) return@setOnItemSelectedListener true
            val currentDest = navController.currentDestination?.id
            if (currentDest == item.itemId) return@setOnItemSelectedListener true
            val startDestId = navController.graph.findStartDestination().id
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(startDestId, inclusive = false)
                .build()
            runCatching {
                navController.navigate(item.itemId, null, options)
            }.onFailure {
                runCatching { navController.navigate(item.itemId) }
            }
            true
        }

        // Sync bottom nav checked state with destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isSyncingNav = true
            binding.bottomNav.menu.setGroupCheckable(0, true, true)
            binding.bottomNav.selectedItemId = destination.id
            isSyncingNav = false
            binding.bottomNav.isVisible = destination.id in navVisibleIds
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    val now = System.currentTimeMillis()
                    if (now - backPressedAt < 2000) {
                        finish()
                    } else {
                        backPressedAt = now
                        Snackbar.make(binding.root, "再按一次返回键退出", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    navController.navigateUp()
                }
            }
        })
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
                "✓ 已自动提交下载：…${url.takeLast(30)}"
            } else {
                "提交失败：${result.exceptionOrNull()?.message}"
            }
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }
}
