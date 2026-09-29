package com.example.randomgallery.android.ui.gif

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.RandomGifVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.common.isExpandedWidth
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.Downloader
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.MediaKind
import com.example.randomgallery.android.ui.wallpaper.WallpaperMedia
import com.example.randomgallery.android.ui.wallpaper.WallpaperPreviewDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun RandomGifScreen(
    onBack: () -> Unit,
    onDetail: (workId: String) -> Unit,
    onAuthor: (authorId: String) -> Unit,
    viewModel: RandomGifViewModel
) {
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val singleGifs by viewModel.gifList.collectAsStateWithLifecycle()
    val groupGifs by viewModel.currentGroupGifs.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isFavorited by viewModel.isFavorited.collectAsStateWithLifecycle()
    var wallpaperTargetMedia by remember { mutableStateOf<WallpaperMedia?>(null) }

    // 状态栏：保持全面屏沉浸式显示（不隐藏状态栏），确保暗色背景下电量/时间/通知图标清晰可见
    val view = LocalView.current
    val hostActivity = LocalContext.current as? Activity
    DisposableEffect(hostActivity) {
        val window = hostActivity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars ?: false
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = previousLightStatusBars
        }
    }

    // ── REQ-02 预测性返回 ──
    // 不用无条件 BackHandler：那会吃掉系统的预测返回预览（manifest 已开 enableOnBackInvokedCallback）。
    // PredictiveBackHandler 在系统手势进行中持续回调 progress，松手完成返回、中断则回弹。
    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { event ->
                backProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            // 手势完成：收尾到终态后真正返回
            backProgress.animateTo(1f, tween(160))
            onBack()
        } catch (_: CancellationException) {
            // 手势中断：回弹
            backProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer {
                val p = backProgress.value
                translationX = -size.width * 0.22f * p
                val s = 1f - 0.08f * p
                scaleX = s
                scaleY = s
            }
    ) {
        if (playMode == "group") {
            CardStackDeckViewer(
                groupGifs = groupGifs,
                loading = loading,
                error = error,
                isFavorited = isFavorited,
                onBack = onBack,
                onDetail = onDetail,
                onAuthor = onAuthor,
                onNextGroup = { viewModel.loadNextGroup() },
                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                onReportDead = { viewModel.reportDead(it) },
                onCheckFavorite = { viewModel.checkFavorite(it) },
                onSwitchMode = { viewModel.switchMode(it) },
                onWallpaper = { gif ->
                    val url = gif.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }.orEmpty()
                    if (url.isNotBlank()) {
                        wallpaperTargetMedia = WallpaperMedia(url = url, isVideo = true, title = gif.workTitle)
                    }
                }
            )
        } else {
            SingleCardDynamicGifViewer(
                gifList = singleGifs,
                loading = loading,
                error = error,
                isFavorited = isFavorited,
                onBack = onBack,
                onDetail = onDetail,
                onAuthor = onAuthor,
                onLoadNext = { viewModel.loadNext() },
                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                onReportDead = { viewModel.reportDead(it) },
                onCheckFavorite = { viewModel.checkFavorite(it) },
                onSwitchMode = { viewModel.switchMode(it) },
                onWallpaper = { gif ->
                    val url = gif.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }.orEmpty()
                    if (url.isNotBlank()) {
                        wallpaperTargetMedia = WallpaperMedia(url = url, isVideo = true, title = gif.workTitle)
                    }
                }
            )
        }
    }

    wallpaperTargetMedia?.let { media ->
        WallpaperPreviewDialog(
            media = media,
            onDismiss = { wallpaperTargetMedia = null }
        )
    }
}

/**
 * REQ-06：展开宽度（≥840dp）下的右侧信息与操作栏。
 * 卡片留在左侧，避免在平板 / 横屏上被拉成超宽横幅。
 */
@Composable
private fun GifInfoSidePanel(
    gif: RandomGifVO?,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onDetail: () -> Unit,
    onAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = gif?.workTitle?.takeIf { it.isNotBlank() } ?: "未命名作品",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            val author = gif?.authorNickname
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Button(
                onClick = onToggleFavorite,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorited) {
                        XhsRed
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isFavorited) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(if (isFavorited) "已收藏" else "收藏", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onDetail,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .bouncyClickable(onClick = onDetail)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text("作品详情", fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = onAuthor,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .bouncyClickable(onClick = onAuthor)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text("作者主页", fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** REQ-06 横屏 / 平板验收预览：展开分栏的右侧面板。 */
@Preview(name = "展开分栏 · 横屏平板", widthDp = 960, heightDp = 460, showBackground = true)
@Composable
private fun GifInfoSidePanelPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            GifInfoSidePanel(
                gif = RandomGifVO(
                    id = 1L,
                    workId = "w1",
                    authorId = "a1",
                    workTitle = "示例作品标题",
                    authorNickname = "示例作者"
                ),
                isFavorited = true,
                onToggleFavorite = {},
                onDetail = {},
                onAuthor = {},
                modifier = Modifier.width(300.dp)
            )
        }
    }
}

// ── 底部沉浸控制栏（全屏沉浸式微渐变，精简至 4 个操作，杜绝色块与拥挤）─────────
//
// 结构分明、呼吸感充足：
//   行 1 → 标题区（可直接点击查看作品详情，带 Chevron 指示）+ 右侧作者 Chip（点击直达作者页）
//   行 2 → 左侧核心操作（换一张 / 下一套）+ 右侧 3 个功能图标（收藏 / 分享 / 保存相册）

@Composable
private fun GifBottomControlBar(
    title: String,
    authorNickname: String?,
    onAuthor: () -> Unit,
    onDetail: (() -> Unit)?,
    nextLabel: String,
    onNext: () -> Unit,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onWallpaper: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 行 1: 标题 + 作品详情入口，右侧作者 Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 作品标题（点击可直达作品详情）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (onDetail != null) {
                                Modifier.bouncyClickable(onClickLabel = "查看作品详情", onClick = onDetail)
                            } else Modifier
                        )
                        .padding(vertical = 2.dp)
                ) {
                    GifTitleText(
                        title = title,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (onDetail != null) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "查看作品详情",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 作者 Chip
                if (!authorNickname.isNullOrBlank()) {
                    GifAuthorChip(
                        nickname = authorNickname,
                        onClick = onAuthor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // 行 2: 主操作「换一张」+ 功能按键（收藏、分享、下载、设为壁纸）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧主要操作：珊瑚红渐变胶囊
                GifPrimaryActionButton(
                    text = nextLabel,
                    onClick = onNext
                )

                // 右侧功能图标组：38dp 圆形磨砂玻璃键
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ❤️ 收藏
                    GifCircleActionButton(
                        contentDescription = if (isFavorited) "取消收藏" else "收藏",
                        active = isFavorited,
                        onClick = onToggleFavorite
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorited) Color(0xFFFF2442) else Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // 分享
                    GifCircleActionButton(
                        contentDescription = "分享",
                        onClick = onShare
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // 保存到相册
                    GifCircleActionButton(
                        contentDescription = "保存到相册",
                        onClick = onSave
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // 设为动态壁纸
                    if (onWallpaper != null) {
                        GifCircleActionButton(
                            contentDescription = stringResource(R.string.wallpaper_action_set_live),
                            onClick = onWallpaper
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wallpaper,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GifTitleText(title: String, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = title,
        modifier = modifier,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "gifTitleChange"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GifAuthorChip(
    nickname: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = nickname,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "gifAuthorChange"
    ) { name ->
        Box(
            modifier = modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(0.5.dp, Color(0xFFFFD54F).copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                .bouncyClickable(onClickLabel = stringResource(R.string.gif_view_author)) { onClick() }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "@$name",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 核心主操作胶囊按钮（换一张 / 下一套），小红书红微渐变，38dp 高度 */
@Composable
private fun GifPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF385C), Color(0xFFFF2442))
                )
            )
            .bouncyClickable(onClickLabel = text, onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

/** 高质感圆形磨砂玻璃功能按钮（收藏 / 分享 / 下载），统一 38dp 规格，弹性回弹，不重叠。 */
@Composable
private fun GifCircleActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeColor: Color = Color(0xFFFF2442),
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (active) activeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.14f)
            )
            .border(
                0.6.dp,
                if (active) activeColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.22f),
                CircleShape
            )
            .bouncyClickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

// ── 底部控制栏布局验收预览 ──────────────────────────────────────────

@Composable
private fun GifBottomControlBarPreviewContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GifBottomControlBar(
            title = "夏日清新微风写真合集",
            authorNickname = "小红书摄影师",
            onAuthor = {},
            onDetail = {},
            nextLabel = "换一张",
            onNext = {},
            isFavorited = true,
            onToggleFavorite = {},
            onShare = {},
            onSave = {},
            onWallpaper = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** 手机竖屏（360dp）：标题+作者一行；主操作与功能图标各居一侧，平滑不重叠。 */
@Preview(name = "底部信息条 · 手机窄屏", widthDp = 360, heightDp = 260, showBackground = true)
@Composable
private fun GifInfoBarCompactPreview() {
    MaterialTheme { GifBottomControlBarPreviewContent() }
}

/** 平板 / 横屏（960dp）：同样的两行结构，主操作贴左、图标操作贴右。 */
@Preview(name = "底部信息条 · 平板宽屏", widthDp = 960, heightDp = 180, showBackground = true)
@Composable
private fun GifInfoBarWidePreview() {
    MaterialTheme { GifBottomControlBarPreviewContent() }
}

// ── 模式 A：单张随机（仅中间卡片手势切换，全屏背景高斯模糊与底部文案动态过渡）──

@OptIn(UnstableApi::class)
@Composable
// REQ-09：放开到 internal 以便 androidTest 通过 friend path 驱动交互
internal fun SingleCardDynamicGifViewer(
    gifList: List<RandomGifVO>,
    loading: Boolean,
    error: String?,
    isFavorited: Boolean,
    onBack: () -> Unit,
    onDetail: (workId: String) -> Unit,
    onAuthor: (authorId: String) -> Unit,
    onLoadNext: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onReportDead: (Long) -> Unit = {},
    onCheckFavorite: (Long) -> Unit = {},
    onSwitchMode: (String) -> Unit,
    onWallpaper: ((RandomGifVO) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    // REQ-08：用 Compose 的 haptic 通道，跨版本、免权限、遵循系统分级设置
    val haptic = LocalHapticFeedback.current

    // REQ-06：旋转 / 分屏等配置变化会重建 Activity，这两项必须可保存
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingAdvance by remember { mutableStateOf(false) }
    var isVideoReady by remember(activeIndex) { mutableStateOf(false) }
    var isVideoFailed by remember(activeIndex) { mutableStateOf(false) }
    var isCoverFailed by remember(activeIndex) { mutableStateOf(false) }
    var videoRatio by remember(activeIndex) { mutableFloatStateOf(0.75f) }
    var isPaused by remember(activeIndex) { mutableStateOf(false) }
    var showHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    // 底部信息条实测高度：窄屏两行栏明显更高，卡片视口据此预留底部空间，避免被信息条遮住
    var infoBarHeightPx by remember { mutableIntStateOf(0) }
    val infoBarBottomInset = if (infoBarHeightPx > 0) {
        with(density) { infoBarHeightPx.toDp() }
    } else {
        68.dp
    }

    LaunchedEffect(gifList.size) {
        if (pendingAdvance && activeIndex + 1 < gifList.size) {
            pendingAdvance = false
            activeIndex += 1
        }
    }

    // 两个 ExoPlayer 轮换（当前 activeIndex 与 activeIndex + 1 预加载）
    val players = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.xiaohongshu.com/"))
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)
        Array(2) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = 0f
                }
        }
    }
    DisposableEffect(Unit) { onDispose { players.forEach { it.release() } } }

    val curPlayer = players[activeIndex % 2]
    val prePlayer = players[(activeIndex + 1) % 2]

    // 生命周期切后台暂停处理，防止耗电与内存泄露
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, curPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> curPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isVideoReady) curPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(curPlayer, activeIndex) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isVideoReady = true
                    isVideoFailed = false
                }
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoRatio = (size.width.toFloat() / size.height).coerceIn(0.55f, 1.8f)
                }
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                // 收到 404/403/连接重置等明确错误，立刻判定失败
                isVideoFailed = true
                isVideoReady = false
            }
        }
        curPlayer.addListener(listener)
        if (curPlayer.playbackState == Player.STATE_READY) {
            isVideoReady = true
            isVideoFailed = false
        }
        if (curPlayer.videoSize.width > 0 && curPlayer.videoSize.height > 0) {
            videoRatio = (curPlayer.videoSize.width.toFloat() / curPlayer.videoSize.height).coerceIn(0.55f, 1.8f)
        }
        onDispose { curPlayer.removeListener(listener) }
    }

    val currentGif = gifList.getOrNull(activeIndex)
    // REQ-06：展开宽度（≥840dp）时右侧保留信息与操作栏
    val isExpanded = isExpandedWidth()
    val sidePanelWidth = if (isExpanded) 300.dp else 0.dp

    // ── REQ-07 分享 / 保存到相册 ──
    fun shareCurrent() {
        val url = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }.orEmpty()
        if (url.isBlank()) {
            Messenger.show("暂无媒体地址可分享", isError = true)
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "分享到"))
    }

    // 复用全 App 唯一的下载入口：它带了 xhscdn / 小红书 的 Referer 头，
    // 自行用 OkHttp + MediaStore 重写会因防盗链 403。
    fun saveCurrent() {
        val url = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }.orEmpty()
        if (url.isBlank()) {
            Messenger.show("暂无媒体地址可保存", isError = true)
            return
        }
        Downloader.enqueue(context, url, MediaKind.VIDEO)
            .onSuccess { Messenger.show("已开始保存到相册") }
            .onFailure { Messenger.show("保存失败：${it.message}", isError = true) }
    }

    // API 29+ 由 DownloadManager 免权限写入公共目录；API 28 需运行时申请
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveCurrent() else Messenger.show("未授予存储权限，无法保存", isError = true)
    }

    fun onSaveClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveCurrent()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    val nextGif = gifList.getOrNull(activeIndex + 1)
    val displayUrl = currentGif?.mediaUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""

    // 监听当前卡片变化，同步更新收藏状态
    LaunchedEffect(currentGif?.id) {
        currentGif?.id?.let { onCheckFavorite(it) }
    }

    val curRaw = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
    val nextRaw = nextGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }

    // 1. 列表接近末尾时触发预加载后续动图（独立效应，不打断当前播放）
    LaunchedEffect(activeIndex, gifList.size) {
        if (gifList.isNotEmpty() && activeIndex >= gifList.size - 2) {
            onLoadNext()
        }
    }

    // 2. 当前视频播放调度：仅当当前视频切换或媒体地址变化时执行，避免后台缓冲时跳回开头
    LaunchedEffect(activeIndex, currentGif?.id, curRaw) {
        if (curRaw.isNullOrBlank()) {
            if (currentGif != null) {
                isVideoFailed = true
            }
            return@LaunchedEffect
        }

        val isSameMedia = curPlayer.currentMediaItem?.mediaId == curRaw
        if (!isSameMedia) {
            isVideoReady = false
            isVideoFailed = false
            isCoverFailed = false
            curPlayer.setMediaItem(MediaItem.fromUri(curRaw))
            curPlayer.prepare()
        } else {
            if (curPlayer.playbackState == Player.STATE_READY) {
                isVideoReady = true
                isVideoFailed = false
            }
        }
        if (!isPaused) {
            curPlayer.play()
        }
    }

    // 3. 下一张动图后台预热：独立执行，静默缓冲下一张视频
    LaunchedEffect(nextRaw) {
        if (!nextRaw.isNullOrBlank() && prePlayer.currentMediaItem?.mediaId != nextRaw) {
            prePlayer.setMediaItem(MediaItem.fromUri(nextRaw))
            prePlayer.prepare()
            prePlayer.pause()
        }
    }

    // 快速失败自动切换机制：无感平滑跳过失效动图，不弹打扰性 Toast
    LaunchedEffect(activeIndex, isVideoFailed, isCoverFailed) {
        if ((isCoverFailed || isVideoFailed) && gifList.isNotEmpty()) {
            currentGif?.id?.let { onReportDead(it) }
            if (activeIndex + 1 < gifList.size) {
                // 缓冲队列中已有下一张：0ms 秒切下一张（无感丝滑体验）
                activeIndex += 1
            } else {
                // 队列暂时见底：标记等待新图拉取到位并触发拉取
                pendingAdvance = true
                onLoadNext()
            }
        }
    }

    // 缓冲超时降级跳过（8秒仍无法播放首帧且非失败，静默跳至下一张）
    LaunchedEffect(activeIndex, isVideoReady, isVideoFailed, isCoverFailed) {
        if (!isVideoReady && !isVideoFailed && !isCoverFailed && gifList.isNotEmpty()) {
            delay(8000)
            if (!isVideoReady && !isVideoFailed && !isCoverFailed) {
                if (activeIndex + 1 < gifList.size) {
                    activeIndex += 1
                } else {
                    pendingAdvance = true
                    onLoadNext()
                }
            }
        }
    }

    // 自动切图等待超时保护：若 8 秒仍未返回新图，重置 pendingAdvance 允许手动轻触重试
    LaunchedEffect(pendingAdvance, activeIndex) {
        if (pendingAdvance) {
            delay(8000)
            if (pendingAdvance && activeIndex + 1 >= gifList.size) {
                pendingAdvance = false
            }
        }
    }

    // 心形爆发动画
    LaunchedEffect(showHeart) {
        if (showHeart) {
            heartScale.snapTo(0f)
            heartScale.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            heartScale.animateTo(0f, tween(300))
            showHeart = false
        }
    }

    // 中间单张卡片垂直拖动手势（REQ-04：改为垂直，避免与系统侧滑返回手势冲突）
    val cardOffsetY = remember { Animatable(0f) }
    // 卡片实测高度：垂直阈值与渐隐比例都基于它，不写死屏幕宽度
    var cardHeightPx by remember { mutableFloatStateOf(1f) }
    // 长按倍速（REQ-03）
    var isSpeedUp by remember { mutableStateOf(false) }

    // REQ-03：两个 ExoPlayer 轮换复用，切图时必须复位，否则 2x 会残留到下一张
    LaunchedEffect(activeIndex) {
        isSpeedUp = false
        players.forEach { it.setPlaybackSpeed(1f) }
    }

    fun applySpeedUp(on: Boolean) {
        if (isSpeedUp == on) return
        isSpeedUp = on
        curPlayer.setPlaybackSpeed(if (on) 2f else 1f)
    }

    // REQ-04：垂直拖动结束的落位判定
    suspend fun settleVerticalDrag() {
        val h = cardHeightPx
        val dy = cardOffsetY.value
        val threshold = h * 0.18f
        when {
            dy < -threshold -> {
                // 上滑 → 下一条
                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                cardOffsetY.animateTo(-h * 1.15f, spring(stiffness = Spring.StiffnessMediumLow))
                cardOffsetY.snapTo(0f)
                if (activeIndex + 1 < gifList.size) {
                    activeIndex += 1
                } else {
                    pendingAdvance = true
                    onLoadNext()
                }
            }
            dy > threshold -> {
                // 下滑 → 上一条（仅历史内）
                if (activeIndex > 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    cardOffsetY.animateTo(h * 1.15f, spring(stiffness = Spring.StiffnessMediumLow))
                    cardOffsetY.snapTo(0f)
                    activeIndex -= 1
                } else {
                    cardOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
            else -> cardOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 1. 全屏背景：动态跨帧渐变，页面框架绝不滑走 ──
        Crossfade(
            targetState = displayUrl,
            animationSpec = tween(300),
            label = "bgFade"
        ) { url ->
            if (url.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        when {
            loading && gifList.isEmpty() -> XhsLoadingBox(Modifier.fillMaxSize())
            error != null && gifList.isEmpty() -> XhsEmptyState(
                error,
                onRetry = onLoadNext,
                modifier = Modifier.fillMaxSize()
            )
            gifList.isNotEmpty() -> {
                // ── 2. 中间卡片独立手势拖拽切图视口 ──
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 24.dp,
                            end = 24.dp + sidePanelWidth,
                            top = statusBarTop + 54.dp,
                            bottom = infoBarBottomInset
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoRatio)
                            .onSizeChanged { cardHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                            .graphicsLayer {
                                translationY = cardOffsetY.value
                                // 垂直拖动时按拖动比例渐隐
                                alpha = 1f - (kotlin.math.abs(cardOffsetY.value) / cardHeightPx * 0.35f).coerceIn(0f, 0.35f)
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .pointerInput(activeIndex) {
                                // REQ-03/04：单击=播放暂停，双击=收藏，长按=2x
                                detectTapGestures(
                                    onTap = {
                                        isPaused = !isPaused
                                        if (isPaused) curPlayer.pause() else curPlayer.play()
                                    },
                                    onDoubleTap = {
                                        showHeart = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentGif?.id?.let(onToggleFavorite)
                                    },
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        applySpeedUp(true)
                                    },
                                    onPress = {
                                        // 拖动或被取消时同样会返回，必须复位倍速
                                        tryAwaitRelease()
                                        applySpeedUp(false)
                                    }
                                )
                            }
                            .pointerInput(activeIndex) {
                                // REQ-04：垂直拖动切换，横向留给系统返回手势
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch { cardOffsetY.snapTo(cardOffsetY.value + dragAmount.y) }
                                    },
                                    onDragEnd = { scope.launch { settleVerticalDrag() } },
                                    onDragCancel = { scope.launch { settleVerticalDrag() } }
                                )
                            }
                            .testTag("gif_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 先验海报底衬
                            if (displayUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(displayUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    onError = { isCoverFailed = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // 播放器
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        this.player = curPlayer
                                    }
                                },
                                update = { view ->
                                    view.useController = false
                                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    if (view.player != curPlayer) view.player = curPlayer
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(alpha = if (isVideoReady) 1f else 0f)
                            )

                            // ❤️ 双击收藏心形爆发动画
                            if (showHeart) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5050),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(80.dp)
                                        .graphicsLayer {
                                            scaleX = heartScale.value
                                            scaleY = heartScale.value
                                            alpha = heartScale.value
                                        }
                                )
                            }

                            // 播放/暂停指示器
                            if (isPaused && isVideoReady) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(64.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "播放",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            // 正在寻找下一张 / 失败重试过渡态（彻底移除惊悚大黑遮罩与“糟糕找不到图片”）
                            if (isVideoFailed || isCoverFailed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (pendingAdvance) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = Color.White.copy(alpha = 0.85f),
                                                strokeWidth = 2.5.dp
                                            )
                                            Text(
                                                text = "正在寻找下一张...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .bouncyClickable {
                                                    pendingAdvance = true
                                                    onLoadNext()
                                                }
                                                .padding(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "重试",
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = "网络连接较慢，轻触重试",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }

                            // REQ-03：长按倍速时的状态提示
                            if (isSpeedUp) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.62f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "2x 快进中",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // REQ-06：展开宽度下卡片留在左侧，右侧承载信息与操作
                    if (isExpanded) {
                        GifInfoSidePanel(
                            gif = currentGif,
                            isFavorited = isFavorited,
                            onToggleFavorite = { currentGif?.id?.let(onToggleFavorite) },
                            onDetail = { currentGif?.workId?.let(onDetail) },
                            onAuthor = { currentGif?.authorId?.let(onAuthor) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(sidePanelWidth)
                        )
                    }
                }

                // 垂直滚动进度指示：显示在卡片外部右侧屏幕边缘，杜绝遮挡图片内容
                if (gifList.size > 1 && !isExpanded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .padding(bottom = infoBarBottomInset)
                            .width(3.5.dp)
                            .height(84.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .testTag("gif_progress_$activeIndex"),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(
                            Modifier
                                .weight(activeIndex.coerceAtLeast(0).toFloat().coerceAtLeast(0.001f))
                                .fillMaxWidth()
                                .background(Color.Transparent)
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.White)
                        )
                        Box(
                            Modifier
                                .weight((gifList.size - activeIndex - 1).coerceAtLeast(0).toFloat().coerceAtLeast(0.001f))
                                .fillMaxWidth()
                                .background(Color.Transparent)
                        )
                    }
                }
            }
        }

        // ── 3. 底部沉浸控制栏 ──
        if (currentGif != null) {
            GifBottomControlBar(
                title = currentGif.workTitle?.ifBlank { "沉浸动图短片" } ?: "沉浸动图短片",
                authorNickname = currentGif.authorNickname,
                onAuthor = { currentGif.authorId?.let(onAuthor) },
                onDetail = currentGif.workId?.let { wid -> { onDetail(wid) } },
                nextLabel = "换一张",
                onNext = {
                    if (activeIndex + 1 < gifList.size) {
                        activeIndex += 1
                    } else {
                        pendingAdvance = true
                        onLoadNext()
                    }
                },
                isFavorited = isFavorited,
                onToggleFavorite = { currentGif?.id?.let(onToggleFavorite) },
                onShare = { shareCurrent() },
                onSave = { onSaveClick() },
                onWallpaper = { currentGif?.let { onWallpaper?.invoke(it) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { infoBarHeightPx = it.height }
            )
        }

        // ── 4. 顶部导航与模式切换（单张模式在右侧显示当前序号胶囊，如 3 / 10）──
        TopHeaderBar(
            currentMode = "single",
            onBack = onBack,
            onSwitchMode = onSwitchMode,
            badgeText = if (gifList.size > 1) "${activeIndex + 1} / ${gifList.size}" else null
        )
    }
}

// ── 模式 B：整组套图扑克牌物理堆叠与双向抽切牌视口 ────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun CardStackDeckViewer(
    groupGifs: List<RandomGifVO>,
    loading: Boolean,
    error: String?,
    isFavorited: Boolean,
    onBack: () -> Unit,
    onDetail: (workId: String) -> Unit,
    onAuthor: (authorId: String) -> Unit,
    onNextGroup: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onReportDead: (Long) -> Unit = {},
    onCheckFavorite: (Long) -> Unit = {},
    onSwitchMode: (String) -> Unit,
    onWallpaper: ((RandomGifVO) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()

    var activeIndex by remember(groupGifs) { mutableIntStateOf(0) }
    var isVideoReady by remember(activeIndex, groupGifs) { mutableStateOf(false) }
    var isVideoFailed by remember(activeIndex, groupGifs) { mutableStateOf(false) }
    var isCoverFailed by remember(activeIndex, groupGifs) { mutableStateOf(false) }

    // 同 SingleCardDynamicGifViewer：底部信息条实测高度，用于卡片视口底部预留
    var infoBarHeightPx by remember { mutableIntStateOf(0) }
    val infoBarBottomInset = if (infoBarHeightPx > 0) {
        with(density) { infoBarHeightPx.toDp() }
    } else {
        68.dp
    }

    val players = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.xiaohongshu.com/"))
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)
        Array(2) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = 0f
                }
        }
    }
    DisposableEffect(Unit) { onDispose { players.forEach { it.release() } } }

    val curPlayer = players[activeIndex % 2]
    val prePlayer = players[(activeIndex + 1) % 2]

    // 生命周期切后台暂停处理
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, curPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> curPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isVideoReady) curPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(curPlayer, activeIndex) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isVideoReady = true
                    isVideoFailed = false
                }
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                isVideoFailed = true
                isVideoReady = false
            }
        }
        curPlayer.addListener(listener)
        if (curPlayer.playbackState == Player.STATE_READY) {
            isVideoReady = true
            isVideoFailed = false
        }
        onDispose { curPlayer.removeListener(listener) }
    }

    val currentGif = groupGifs.getOrNull(activeIndex)
    val nextGif = groupGifs.getOrNull(activeIndex + 1)
    val displayUrl = currentGif?.mediaUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""

    fun shareCurrent() {
        val url = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }.orEmpty()
        if (url.isBlank()) {
            Messenger.show("暂无媒体地址可分享", isError = true)
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "分享到"))
    }

    fun saveCurrent() {
        val url = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }.orEmpty()
        if (url.isBlank()) {
            Messenger.show("暂无媒体地址可保存", isError = true)
            return
        }
        Downloader.enqueue(context, url, MediaKind.VIDEO)
            .onSuccess { Messenger.show("已开始保存到相册") }
            .onFailure { Messenger.show("保存失败：${it.message}", isError = true) }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveCurrent() else Messenger.show("未授予存储权限，无法保存", isError = true)
    }

    fun onSaveClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveCurrent()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val curRaw = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
    val nextRaw = nextGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }

    LaunchedEffect(activeIndex, currentGif?.id, curRaw) {
        if (curRaw.isNullOrBlank()) {
            if (currentGif != null) {
                isVideoFailed = true
            }
            return@LaunchedEffect
        }

        val isSameMedia = curPlayer.currentMediaItem?.mediaId == curRaw
        if (!isSameMedia) {
            isVideoReady = false
            isVideoFailed = false
            isCoverFailed = false
            curPlayer.setMediaItem(MediaItem.fromUri(curRaw))
            curPlayer.prepare()
        } else {
            if (curPlayer.playbackState == Player.STATE_READY) {
                isVideoReady = true
                isVideoFailed = false
            }
        }
        curPlayer.play()
    }

    LaunchedEffect(nextRaw) {
        if (!nextRaw.isNullOrBlank() && prePlayer.currentMediaItem?.mediaId != nextRaw) {
            prePlayer.setMediaItem(MediaItem.fromUri(nextRaw))
            prePlayer.prepare()
            prePlayer.pause()
        }
    }

    val currentGroupGif = groupGifs.getOrNull(activeIndex)
    LaunchedEffect(currentGroupGif?.id) {
        currentGroupGif?.id?.let { onCheckFavorite(it) }
    }

    // 快速跳过判定（无感静默跳过失效动图，不弹打扰性 Toast）
    LaunchedEffect(activeIndex, isVideoFailed, isCoverFailed, groupGifs.size) {
        if ((isCoverFailed || isVideoFailed) && groupGifs.isNotEmpty()) {
            val cur = groupGifs.getOrNull(activeIndex)
            cur?.id?.let { onReportDead(it) }
            if (activeIndex + 1 < groupGifs.size) {
                activeIndex += 1
            } else {
                onNextGroup()
            }
        }
    }

    // 缓冲超时静默跳过
    LaunchedEffect(activeIndex, isVideoReady, isVideoFailed, isCoverFailed, groupGifs.size) {
        if (!isVideoReady && !isVideoFailed && !isCoverFailed && groupGifs.isNotEmpty()) {
            delay(8000)
            if (!isVideoReady && !isVideoFailed && !isCoverFailed) {
                if (activeIndex + 1 < groupGifs.size) {
                    activeIndex += 1
                } else {
                    onNextGroup()
                }
            }
        }
    }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = displayUrl,
            animationSpec = tween(300),
            label = "groupBgFade"
        ) { url ->
            if (url.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }
        }

        when {
            loading && groupGifs.isEmpty() -> XhsLoadingBox(Modifier.fillMaxSize())
            error != null && groupGifs.isEmpty() -> XhsEmptyState(
                error,
                onRetry = onNextGroup,
                modifier = Modifier.fillMaxSize()
            )
            groupGifs.isNotEmpty() -> {
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = statusBarTop + 54.dp, bottom = infoBarBottomInset),
                    contentAlignment = Alignment.Center
                ) {
                    val remainingCount = (groupGifs.size - activeIndex).coerceAtLeast(0)
                    val visibleCards = (0 until minOf(3, remainingCount)).reversed()

                    visibleCards.forEach { stackOffset ->
                        val cardIndex = activeIndex + stackOffset
                        val gif = groupGifs[cardIndex]
                        val isTopCard = stackOffset == 0

                        val baseRotation = when (stackOffset) {
                            0 -> 0f
                            1 -> -3.5f
                            2 -> 4.2f
                            else -> 0f
                        }
                        val baseScale = 1f - (stackOffset * 0.05f)
                        val baseOffsetY = (stackOffset * 14).dp
                        val dragRotation = if (isTopCard) (offsetX.value / screenWidthPx) * 20f else 0f

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            shadowElevation = (12 - stackOffset * 3).dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = if (isTopCard) 0.35f else 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .offset(y = baseOffsetY)
                                .graphicsLayer {
                                    scaleX = baseScale
                                    scaleY = baseScale
                                    translationX = if (isTopCard) offsetX.value else 0f
                                    translationY = if (isTopCard) offsetY.value else 0f
                                    rotationZ = baseRotation + dragRotation
                                }
                                .zIndex((3 - stackOffset).toFloat())
                                .clip(RoundedCornerShape(24.dp))
                                .then(
                                    if (isTopCard) {
                                        Modifier.pointerInput(cardIndex) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    scope.launch {
                                                        if (offsetX.value < -screenWidthPx * 0.22f) {
                                                            offsetX.animateTo(-screenWidthPx * 1.3f, spring(stiffness = Spring.StiffnessMediumLow))
                                                            offsetX.snapTo(0f)
                                                            offsetY.snapTo(0f)
                                                            if (activeIndex + 1 < groupGifs.size) {
                                                                activeIndex += 1
                                                            } else {
                                                                onNextGroup()
                                                            }
                                                        } else if (offsetX.value > screenWidthPx * 0.22f) {
                                                            if (activeIndex > 0) {
                                                                offsetX.animateTo(screenWidthPx * 1.3f, spring(stiffness = Spring.StiffnessMediumLow))
                                                                offsetX.snapTo(0f)
                                                                offsetY.snapTo(0f)
                                                                activeIndex -= 1
                                                            } else {
                                                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                            }
                                                        } else {
                                                            offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                        }
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    scope.launch {
                                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                                        offsetY.snapTo(offsetY.value + dragAmount.y * 0.25f)
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val bgCover = gif.mediaUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""

                                if (bgCover.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(bgCover)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        onError = {
                                            if (isTopCard) isCoverFailed = true
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (isTopCard) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                useController = false
                                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                layoutParams = ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                this.player = curPlayer
                                            }
                                        },
                                        update = { view ->
                                            view.useController = false
                                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            if (view.player != curPlayer) view.player = curPlayer
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(alpha = if (isVideoReady) 1f else 0f)
                                    )

                                    if (isVideoFailed || isCoverFailed) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(32.dp),
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    strokeWidth = 2.5.dp
                                                )
                                                Text(
                                                    text = if (activeIndex + 1 < groupGifs.size) "正在切换下一张..." else "正在换一套...",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Collections,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${cardIndex + 1} / ${groupGifs.size} 抽",
                                            style = MaterialTheme.typography.labelSmall.tabularNumbers,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. 底部沉浸控制栏 ──
        if (currentGif != null) {
            GifBottomControlBar(
                title = currentGif.workTitle?.ifBlank { "沉浸动图套图" } ?: "沉浸动图套图",
                authorNickname = currentGif.authorNickname,
                onAuthor = { currentGif.authorId?.let(onAuthor) },
                onDetail = currentGif.workId?.let { wid -> { onDetail(wid) } },
                nextLabel = if (activeIndex + 1 < groupGifs.size) "下一张" else "下一套",
                onNext = {
                    if (activeIndex + 1 < groupGifs.size) activeIndex += 1
                    else onNextGroup()
                },
                isFavorited = isFavorited,
                onToggleFavorite = { currentGif?.id?.let(onToggleFavorite) },
                onShare = { shareCurrent() },
                onSave = { onSaveClick() },
                onWallpaper = { currentGif?.let { onWallpaper?.invoke(it) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { infoBarHeightPx = it.height }
            )
        }

        TopHeaderBar(
            currentMode = "group",
            onBack = onBack,
            onSwitchMode = onSwitchMode,
            onShuffleClick = onNextGroup,
            shuffleLabel = "换一套"
        )
    }
}

// ── 顶部公共导航栏 ───────────────────────────────────────────────────

@Composable
private fun TopHeaderBar(
    currentMode: String,
    onBack: () -> Unit,
    onSwitchMode: (String) -> Unit,
    onShuffleClick: (() -> Unit)? = null,
    shuffleLabel: String? = null,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 返回按钮：38dp 圆形磨砂玻璃键
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .bouncyClickable(onClickLabel = stringResource(R.string.common_back), onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 模式切换胶囊（单张随机 | 整组套图）
            GifModeSegmentedPill(
                currentMode = currentMode,
                onModeChange = onSwitchMode
            )
        }

        // 右侧操作或指示（套图模式显示「换一套」，单张模式显示数字序号胶囊，如 "3 / 10"）
        if (onShuffleClick != null && !shuffleLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.Black.copy(alpha = 0.50f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(19.dp))
                    .bouncyClickable(onClickLabel = shuffleLabel, onClick = onShuffleClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = shuffleLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.Black.copy(alpha = 0.50f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(19.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Filled.Collections,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium.tabularNumbers,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ── 动图播放模式切换胶囊 Tab ─────────────────────────────────────────

@Composable
private fun GifModeSegmentedPill(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color.Black.copy(alpha = 0.50f))
            .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(19.dp))
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSingle = currentMode == "single"
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (isSingle) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF385C), Color(0xFFFF2442))
                                )
                            )
                        } else Modifier
                    )
                    .bouncyClickable { onModeChange("single") }
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "单张随机",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSingle) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSingle) Color.White else Color.White.copy(alpha = 0.75f)
                )
            }

            val isGroup = currentMode == "group"
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (isGroup) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF385C), Color(0xFFFF2442))
                                )
                            )
                        } else Modifier
                    )
                    .bouncyClickable { onModeChange("group") }
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "整组套图",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isGroup) FontWeight.Bold else FontWeight.Normal,
                    color = if (isGroup) Color.White else Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}
