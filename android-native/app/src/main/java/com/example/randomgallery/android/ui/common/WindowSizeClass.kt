package com.example.randomgallery.android.ui.common

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * REQ-06：大屏 / 横屏判定的统一入口。
 *
 * 替代各处 ad-hoc 的 `configuration.screenWidthDp >= 600`：该阈值既非 Material 分档标准，
 * 也反映不了 Android 16 在 ≥600dp 设备上忽略应用方向 / 可缩放限制后的真实窗口尺寸。
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    val activity = LocalContext.current as? Activity
    return if (activity == null) {
        WindowWidthSizeClass.Compact
    } else {
        calculateWindowSizeClass(activity).widthSizeClass
    }
}

/** 展开宽度（Material 分档 ≥840dp）：平板横屏 / 桌面级窗口，适合分栏布局。 */
@Composable
fun isExpandedWidth(): Boolean = rememberWindowWidthSizeClass() == WindowWidthSizeClass.Expanded
