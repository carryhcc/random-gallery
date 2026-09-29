package com.example.randomgallery.android.ui.wallpaper

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.Spacing
import com.example.randomgallery.android.wallpaper.WallpaperHelper
import com.example.randomgallery.android.wallpaper.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WallpaperMedia(
    val url: String,
    val isVideo: Boolean = false,
    val title: String? = null
)

enum class WallpaperPreviewMode {
    LOCK_SCREEN,
    HOME_SCREEN,
    CLEAN
}

/**
 * 全屏透传壁纸预览与设置组件 (WallpaperPreviewDialog)
 *
 * 核心功能：
 * 1. 全屏沉浸透传：直观预览壁纸应用到屏幕的实际效果（支持锁屏大时钟、桌面 4x4 应用排布、纯净模式）。
 * 2. 交互缩放平移：支持双指缩放、单指平移、双击智能切换、重置与虚化调节。
 * 3. 静态壁纸设置：高保真逆向视口裁切，支持桌面、锁屏或双屏应用。
 * 4. 动态壁纸服务：支持实况短片/视频循环预览与通过系统 Live Wallpaper 预览器一键应用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPreviewDialog(
    media: WallpaperMedia,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // 沉浸式透明状态栏
        val view = LocalView.current
        val hostActivity = context as? Activity
        DisposableEffect(hostActivity) {
            val window = hostActivity?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val prevLightStatus = controller?.isAppearanceLightStatusBars ?: false
            val prevLightNav = controller?.isAppearanceLightNavigationBars ?: false
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
            onDispose {
                controller?.isAppearanceLightStatusBars = prevLightStatus
                controller?.isAppearanceLightNavigationBars = prevLightNav
            }
        }

        // 当前预览模态
        var previewMode by remember { mutableStateOf(WallpaperPreviewMode.LOCK_SCREEN) }
        var showUiControls by remember { mutableStateOf(true) }

        // 视口尺寸测量
        var viewportSize by remember { mutableStateOf(IntSize.Zero) }

        // 静态图片手势变换状态
        var scale by remember { mutableFloatStateOf(1.0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var blurLevel by remember { mutableFloatStateOf(0f) } // 0 ~ 25dp
        var showBlurSlider by remember { mutableStateOf(false) }

        // 目标选择 BottomSheet
        var showTargetSheet by remember { mutableStateOf(false) }
        var isSettingInProgress by remember { mutableStateOf(false) }
        var progressMessage by remember { mutableStateOf("") }

        // 原图 Bitmap 加载缓存
        var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(media.url) {
            if (!media.isVideo && media.url.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    sourceBitmap = WallpaperHelper.loadSourceBitmap(context, media.url)
                }
            }
        }

        // 动态视频播放器
        val player = remember(media.url, media.isVideo) {
            if (media.isVideo && media.url.isNotBlank()) {
                val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .setDefaultRequestProperties(mapOf("Referer" to "https://www.xiaohongshu.com/"))
                val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(httpFactory)
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build().apply {
                        repeatMode = Player.REPEAT_MODE_ONE
                        volume = 0f
                        playWhenReady = true
                        setMediaItem(MediaItem.fromUri(media.url))
                        prepare()
                    }
            } else null
        }
        DisposableEffect(player) {
            onDispose { player?.release() }
        }

        // 实时时钟与日期
        var currentTimeStr by remember { mutableStateOf("") }
        var currentDateStr by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFmt = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
            while (true) {
                val now = Date()
                currentTimeStr = timeFmt.format(now)
                currentDateStr = dateFmt.format(now)
                kotlinx.coroutines.delay(1000)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { viewportSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (showBlurSlider) {
                                showBlurSlider = false
                            } else {
                                showUiControls = !showUiControls
                            }
                        },
                        onDoubleTap = {
                            if (!media.isVideo) {
                                scale = if (scale > 1.2f) 1.0f else 2.0f
                                if (scale == 1.0f) offset = Offset.Zero
                            }
                        }
                    )
                }
        ) {
            // ── 1. 壁纸内容层（带全屏手势缩放与拖拽）────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!media.isVideo) {
                            Modifier.pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1.0f, 4.0f)
                                    scale = newScale
                                    // 限制平移范围
                                    val maxOffsetX = (viewportSize.width * (scale - 1f)) / 2f
                                    val maxOffsetY = (viewportSize.height * (scale - 1f)) / 2f
                                    val newX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    val newY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    offset = Offset(newX, newY)
                                }
                            }
                        } else Modifier
                    )
            ) {
                if (media.isVideo) {
                    // 动态壁纸视频全屏播放
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                this.player = player
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 静态图片预览
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(media.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .then(if (blurLevel > 0.5f) Modifier.blur(blurLevel.dp) else Modifier)
                    )
                }
            }

            // ── 2. 模拟系统透传预览层 (Lock Screen / Home Screen Overlay) ──────────
            AnimatedVisibility(
                visible = previewMode == WallpaperPreviewMode.LOCK_SCREEN && showUiControls,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                LockScreenOverlay(
                    timeStr = currentTimeStr,
                    dateStr = currentDateStr
                )
            }

            AnimatedVisibility(
                visible = previewMode == WallpaperPreviewMode.HOME_SCREEN && showUiControls,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                HomeScreenOverlay()
            }

            // ── 3. 顶部导航与关闭按键 ─────────────────────────────────────────
            AnimatedVisibility(
                visible = showUiControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = Color.White
                        )
                    }

                    // 顶部预览标题药丸
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Wallpaper,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (media.isVideo) stringResource(R.string.wallpaper_action_set_live) else stringResource(R.string.wallpaper_preview_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // 虚化微调开关（仅静态图）
                    if (!media.isVideo) {
                        IconButton(
                            onClick = { showBlurSlider = !showBlurSlider },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (blurLevel > 0f) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                Icons.Filled.BlurOn,
                                contentDescription = stringResource(R.string.wallpaper_blur),
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(Modifier.size(42.dp))
                    }
                }
            }

            // ── 4. 虚化滑块浮窗（展开时展示）───────────────────────────────────
            AnimatedVisibility(
                visible = showBlurSlider && showUiControls && !media.isVideo,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 120.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.80f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "壁纸虚化程度: ${blurLevel.toInt()} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = { blurLevel = 0f }) {
                                Text("清除虚化", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Slider(
                            value = blurLevel,
                            onValueChange = { blurLevel = it },
                            valueRange = 0f..25f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // ── 5. 底部悬浮控制栏（模式切换 + 重置 + 设置主按钮）─────────────
            AnimatedVisibility(
                visible = showUiControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.md, vertical = Spacing.md)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 模态切换胶囊：[锁屏] [桌面] [纯净]
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ModeSwitchPill(
                                label = stringResource(R.string.wallpaper_mode_lock),
                                icon = Icons.Filled.Lock,
                                selected = previewMode == WallpaperPreviewMode.LOCK_SCREEN,
                                onClick = { previewMode = WallpaperPreviewMode.LOCK_SCREEN }
                            )
                            ModeSwitchPill(
                                label = stringResource(R.string.wallpaper_mode_home),
                                icon = Icons.Filled.Home,
                                selected = previewMode == WallpaperPreviewMode.HOME_SCREEN,
                                onClick = { previewMode = WallpaperPreviewMode.HOME_SCREEN }
                            )
                            ModeSwitchPill(
                                label = stringResource(R.string.wallpaper_mode_clean),
                                icon = Icons.Filled.Visibility,
                                selected = previewMode == WallpaperPreviewMode.CLEAN,
                                onClick = { previewMode = WallpaperPreviewMode.CLEAN }
                            )
                        }
                    }

                    // 主操作栏：重置按键 + 「设为壁纸」主按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!media.isVideo) {
                            IconButton(
                                onClick = {
                                    scale = 1.0f
                                    offset = Offset.Zero
                                    blurLevel = 0f
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Filled.RestartAlt,
                                    contentDescription = stringResource(R.string.wallpaper_reset),
                                    tint = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (media.isVideo) {
                                    // 动态壁纸直接准备并唤起选择器
                                    isSettingInProgress = true
                                    progressMessage = context.getString(R.string.wallpaper_preparing_live)
                                    scope.launch {
                                        WallpaperHelper.prepareLiveWallpaper(context, media.url)
                                            .onSuccess {
                                                isSettingInProgress = false
                                                WallpaperHelper.launchLiveWallpaperChooser(context)
                                                    .onSuccess {
                                                        Messenger.show(context.getString(R.string.wallpaper_live_guide))
                                                        onDismiss()
                                                    }
                                                    .onFailure {
                                                        Messenger.show(context.getString(R.string.wallpaper_setting_failed), isError = true)
                                                    }
                                            }
                                            .onFailure {
                                                isSettingInProgress = false
                                                Messenger.show(context.getString(R.string.wallpaper_setting_failed), isError = true)
                                            }
                                    }
                                } else {
                                    // 静态壁纸弹出目标选择菜单
                                    showTargetSheet = true
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(
                                Icons.Filled.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (media.isVideo) stringResource(R.string.wallpaper_action_set_live) else stringResource(R.string.wallpaper_apply),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── 6. 静态壁纸目标选择 BottomSheet ─────────────────────────────
            if (showTargetSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showTargetSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.wallpaper_action_set),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))

                        WallpaperTargetOptionItem(
                            icon = Icons.Filled.Home,
                            title = stringResource(R.string.wallpaper_apply_home),
                            onClick = {
                                showTargetSheet = false
                                applyStatic(context, scope, sourceBitmap, viewportSize, scale, offset, blurLevel, WallpaperTarget.HOME) {
                                    isSettingInProgress = it
                                    if (!it) onDismiss()
                                }
                            }
                        )

                        WallpaperTargetOptionItem(
                            icon = Icons.Filled.Lock,
                            title = stringResource(R.string.wallpaper_apply_lock),
                            onClick = {
                                showTargetSheet = false
                                applyStatic(context, scope, sourceBitmap, viewportSize, scale, offset, blurLevel, WallpaperTarget.LOCK) {
                                    isSettingInProgress = it
                                    if (!it) onDismiss()
                                }
                            }
                        )

                        WallpaperTargetOptionItem(
                            icon = Icons.Filled.PhoneAndroid,
                            title = stringResource(R.string.wallpaper_apply_both),
                            onClick = {
                                showTargetSheet = false
                                applyStatic(context, scope, sourceBitmap, viewportSize, scale, offset, blurLevel, WallpaperTarget.BOTH) {
                                    isSettingInProgress = it
                                    if (!it) onDismiss()
                                }
                            }
                        )

                        Spacer(Modifier.height(Spacing.sm))
                    }
                }
            }

            // ── 7. 设置壁纸执行中进度遮罩 ─────────────────────────────────────
            if (isSettingInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.70f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = progressMessage.ifBlank { stringResource(R.string.wallpaper_setting_progress) },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 执行静态壁纸裁切与设置 */
private fun applyStatic(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    sourceBitmap: Bitmap?,
    viewportSize: IntSize,
    scale: Float,
    offset: Offset,
    blurLevel: Float,
    target: WallpaperTarget,
    onProgress: (Boolean) -> Unit
) {
    if (sourceBitmap == null) {
        Messenger.show(context.getString(R.string.wallpaper_setting_failed), isError = true)
        return
    }
    onProgress(true)
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            val cropped = WallpaperHelper.cropAndTransformBitmap(
                source = sourceBitmap,
                viewportWidth = viewportSize.width,
                viewportHeight = viewportSize.height,
                scale = scale,
                offsetX = offset.x,
                offsetY = offset.y,
                blurRadius = blurLevel
            )
            WallpaperHelper.applyStaticWallpaper(context, cropped, target)
        }
        onProgress(false)
        result.onSuccess {
            Messenger.show(context.getString(R.string.wallpaper_setting_success))
        }.onFailure {
            Messenger.show(context.getString(R.string.wallpaper_setting_failed), isError = true)
        }
    }
}

// ── 模态切换胶囊 Pill ───────────────────────────────────────────────

@Composable
private fun ModeSwitchPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ── 锁屏模式拟真覆盖层 ───────────────────────────────────────────────

@Composable
private fun LockScreenOverlay(
    timeStr: String,
    dateStr: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部时钟区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = timeStr.ifBlank { "12:00" },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp
                ),
                color = Color.White.copy(alpha = 0.95f)
            )
            Text(
                text = dateStr.ifBlank { "9月17日 星期四" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f)
            )
            Spacer(Modifier.height(24.dp))

            // 模拟锁屏通知卡片
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.18f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Wallpaper,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "随机图库 · 壁纸透传预览",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "双指缩放或拖拽平移，实时查看锁屏与遮挡效果",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 底部快捷按键（手电筒 / 向上轻扫解锁 / 相机）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.FlashlightOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.ExpandLess,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "向上滑动解锁",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── 桌面模式拟真覆盖层 ───────────────────────────────────────────────

@Composable
private fun HomeScreenOverlay() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部搜索/小组件栏
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.22f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "搜索应用与内容…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }
        }

        // 中间 4x4 应用图标网格
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            val appRows = listOf(
                listOf(Icons.Filled.PhoneAndroid to "电话", Icons.Filled.ChatBubble to "信息", Icons.Filled.Explore to "浏览器", Icons.Filled.Image to "相册"),
                listOf(Icons.Filled.Settings to "设置", Icons.Filled.AccessTime to "时钟", Icons.Filled.FolderSpecial to "文件", Icons.Filled.AutoAwesome to "相机")
            )
            appRows.forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowApps.forEach { (icon, name) ->
                        AppMockIcon(icon = icon, label = name)
                    }
                }
            }
        }

        // 底部 Dock 栏 (4个常用应用)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.16f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 136.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AppMockIcon(icon = Icons.Filled.PhoneAndroid, label = "电话")
                AppMockIcon(icon = Icons.Filled.ChatBubble, label = "信息")
                AppMockIcon(icon = Icons.Filled.Explore, label = "浏览器")
                AppMockIcon(icon = Icons.Filled.Image, label = "相册")
            }
        }
    }
}

@Composable
private fun AppMockIcon(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.90f),
            maxLines = 1
        )
    }
}

@Composable
private fun WallpaperTargetOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
