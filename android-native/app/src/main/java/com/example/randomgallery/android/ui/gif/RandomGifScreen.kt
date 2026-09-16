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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    // ── REQ-05 沉浸式全屏：进入隐藏系统栏，离开必定恢复 ──
    // 不采用"点击切换系统栏"方案，避免与单击播放/暂停手势冲突。
    val view = LocalView.current
    val hostActivity = LocalContext.current as? Activity
    DisposableEffect(hostActivity) {
        val window = hostActivity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
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
                onBack = onBack,
                onDetail = onDetail,
                onAuthor = onAuthor,
                onNextGroup = { viewModel.loadNextGroup() },
                onSwitchMode = { viewModel.switchMode(it) }
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
                onToggleFavorite = { viewModel.toggleFavorite() },
                onSwitchMode = { viewModel.switchMode(it) }
            )
        }
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorited) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isFavorited) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(if (isFavorited) "已收藏" else "收藏")
            }

            OutlinedButton(onClick = onDetail, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text("作品详情")
            }

            OutlinedButton(onClick = onAuthor, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text("作者主页")
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
    onToggleFavorite: () -> Unit,
    onSwitchMode: (String) -> Unit
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
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var showHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    val cardScale = remember { Animatable(1f) }

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

    // 播放与预热
    LaunchedEffect(activeIndex, gifList.size) {
        if (gifList.isEmpty()) return@LaunchedEffect
        if (activeIndex >= gifList.size - 2) {
            onLoadNext()
        }
        isVideoReady = false
        isVideoFailed = false
        isCoverFailed = false

        val curRaw = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
        val nextRaw = nextGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }

        if (!curRaw.isNullOrBlank()) {
            curPlayer.setMediaItem(MediaItem.fromUri(curRaw))
            curPlayer.prepare()
            curPlayer.play()
        } else {
            isVideoFailed = true
        }

        if (!nextRaw.isNullOrBlank()) {
            prePlayer.setMediaItem(MediaItem.fromUri(nextRaw))
            prePlayer.prepare()
            prePlayer.pause()
        }
    }

    // 快速失败机制：带有当前索引防护，避免跳过竞态
    val targetIndexForCheck = activeIndex
    LaunchedEffect(targetIndexForCheck, isVideoReady, isVideoFailed, isCoverFailed) {
        if (isCoverFailed && activeIndex == targetIndexForCheck) {
            isVideoFailed = true
            Messenger.show("资源已失效，自动切换...", isError = true)
            delay(500)
            if (activeIndex == targetIndexForCheck) {
                if (activeIndex + 1 < gifList.size) activeIndex += 1
                else onLoadNext()
            }
            return@LaunchedEffect
        }

        if (isVideoFailed && activeIndex == targetIndexForCheck) {
            Messenger.show("动图无法播放，自动切换...", isError = true)
            delay(600)
            if (activeIndex == targetIndexForCheck) {
                if (activeIndex + 1 < gifList.size) activeIndex += 1
                else onLoadNext()
            }
            return@LaunchedEffect
        }

        // 缓冲等待时间放宽至 8.0 秒，保障弱网与移动 CDN 首帧体验
        if (!isVideoReady && !isVideoFailed && gifList.isNotEmpty()) {
            delay(8000)
            if (!isVideoReady && activeIndex == targetIndexForCheck) {
                isVideoFailed = true
                Messenger.show("缓冲超时，自动跳过...", isError = true)
                delay(600)
                if (activeIndex == targetIndexForCheck) {
                    if (activeIndex + 1 < gifList.size) activeIndex += 1
                    else {
                        pendingAdvance = true
                        onLoadNext()
                    }
                }
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

    // 点击缩放动画
    LaunchedEffect(isVideoReady) {
        if (isVideoReady) {
            cardScale.snapTo(0.95f)
            cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                if (activeIndex + 1 < gifList.size) activeIndex += 1 else onLoadNext()
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 24.dp,
                            end = 24.dp + sidePanelWidth,
                            top = 68.dp,
                            bottom = 68.dp
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
                                scaleX = cardScale.value
                                scaleY = cardScale.value
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
                                        scope.launch {
                                            cardScale.animateTo(0.94f, tween(70))
                                            cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                        }
                                    },
                                    onDoubleTap = {
                                        showHeart = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleFavorite()
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

                            // 失败提示遮罩
                            if (isVideoFailed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "糟糕，找不到图片了...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "即将自动切换下一张",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
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

                            // REQ-04：右缘垂直进度指示（当前在已加载序列中的位置）
                            if (gifList.size > 1) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 6.dp)
                                        .width(3.dp)
                                        .fillMaxHeight(0.42f)
                                        .clip(CircleShape)
                                        .testTag("gif_progress_$activeIndex"),
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    // weight 必须为正：首/末张时为 0，用极小值兜底
                                    Box(
                                        Modifier
                                            .weight(activeIndex.coerceAtLeast(0).toFloat().coerceAtLeast(0.001f))
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.30f))
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
                                            .background(Color.White.copy(alpha = 0.18f))
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
                            onToggleFavorite = onToggleFavorite,
                            onDetail = { currentGif?.workId?.let(onDetail) },
                            onAuthor = { currentGif?.authorId?.let(onAuthor) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(sidePanelWidth)
                        )
                    }
                }
            }
        }

        // ── 3. 底部半透明悬浮信息卡片 ──
        if (currentGif != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.md, vertical = 20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentGif.workTitle?.ifBlank { "沉浸动图短片" } ?: "沉浸动图短片",
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "titleChange"
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!currentGif.authorNickname.isNullOrBlank()) {
                            AnimatedContent(
                                targetState = currentGif.authorNickname,
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "authorChange"
                            ) { nickname ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    // 触摸目标补到 48dp 下限，并合并语义让 TalkBack 作为单个按钮朗读
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .semantics(mergeDescendants = true) {
                                            role = Role.Button
                                        }
                                        .bouncyClickable(
                                            onClickLabel = stringResource(R.string.gif_view_author)
                                        ) { currentGif.authorId?.let { onAuthor(it) } }
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "@$nickname",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeIndex > 0) {
                            OutlinedButton(
                                onClick = { activeIndex -= 1 },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("上一张", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                if (activeIndex + 1 < gifList.size) activeIndex += 1
                                else {
                                    pendingAdvance = true
                                    onLoadNext()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("换一张", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        if (!currentGif.workId.isNullOrBlank()) {
                            Button(
                                onClick = { onDetail(currentGif.workId) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("看作品", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // ❤️ 收藏按钮
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFavorited) Color(0x33FF5050)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (isFavorited) Color(0xFFFF5050) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // REQ-07：分享原始媒体链接（纯文本，无需 FileProvider）
                        IconButton(
                            onClick = { shareCurrent() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "分享",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // REQ-07：保存到相册（复用 Downloader，保留 XHS Referer 头）
                        IconButton(
                            onClick = { onSaveClick() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save),
                                contentDescription = "保存到相册",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 4. 顶部导航与模式切换 ──
        TopHeaderBar(
            currentMode = "single",
            onBack = onBack,
            onSwitchMode = onSwitchMode,
            onShuffleClick = {
                if (activeIndex + 1 < gifList.size) activeIndex += 1
                else {
                    pendingAdvance = true
                    onLoadNext()
                }
            },
            shuffleLabel = "换一个"
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
    onBack: () -> Unit,
    onDetail: (workId: String) -> Unit,
    onAuthor: (authorId: String) -> Unit,
    onNextGroup: () -> Unit,
    onSwitchMode: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()

    var activeIndex by remember(groupGifs) { mutableIntStateOf(0) }
    var isVideoReady by remember(activeIndex, groupGifs) { mutableStateOf(false) }
    var isVideoFailed by remember(activeIndex, groupGifs) { mutableStateOf(false) }
    var isCoverFailed by remember(activeIndex, groupGifs) { mutableStateOf(false) }

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
        onDispose { curPlayer.removeListener(listener) }
    }

    val currentGif = groupGifs.getOrNull(activeIndex)
    val nextGif = groupGifs.getOrNull(activeIndex + 1)
    val displayUrl = currentGif?.mediaUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""

    LaunchedEffect(activeIndex, groupGifs) {
        if (groupGifs.isEmpty()) return@LaunchedEffect
        isVideoReady = false
        isVideoFailed = false
        isCoverFailed = false

        val curRaw = currentGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
        val nextRaw = nextGif?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }

        if (!curRaw.isNullOrBlank()) {
            curPlayer.setMediaItem(MediaItem.fromUri(curRaw))
            curPlayer.prepare()
            curPlayer.play()
        } else {
            isVideoFailed = true
        }

        if (!nextRaw.isNullOrBlank()) {
            prePlayer.setMediaItem(MediaItem.fromUri(nextRaw))
            prePlayer.prepare()
            prePlayer.pause()
        }
    }

    // 快速跳过判定：带有 targetIndex 防护
    val targetIndexForGroupCheck = activeIndex
    LaunchedEffect(targetIndexForGroupCheck, isVideoReady, isVideoFailed, isCoverFailed, groupGifs) {
        if (isCoverFailed && activeIndex == targetIndexForGroupCheck) {
            isVideoFailed = true
            Messenger.show("资源已失效，自动跳过...", isError = true)
            delay(500)
            if (activeIndex == targetIndexForGroupCheck) {
                if (activeIndex + 1 < groupGifs.size) activeIndex += 1
                else onNextGroup()
            }
            return@LaunchedEffect
        }

        if (isVideoFailed && activeIndex == targetIndexForGroupCheck) {
            Messenger.show("动图无法播放，自动跳过...", isError = true)
            delay(600)
            if (activeIndex == targetIndexForGroupCheck) {
                if (activeIndex + 1 < groupGifs.size) activeIndex += 1
                else onNextGroup()
            }
            return@LaunchedEffect
        }

        if (!isVideoReady && !isVideoFailed && groupGifs.isNotEmpty()) {
            delay(8000)
            if (!isVideoReady && activeIndex == targetIndexForGroupCheck) {
                isVideoFailed = true
                Messenger.show("缓冲超时，自动跳过...", isError = true)
                delay(600)
                if (activeIndex == targetIndexForGroupCheck) {
                    if (activeIndex + 1 < groupGifs.size) activeIndex += 1
                    else onNextGroup()
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 68.dp),
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

                                    if (isVideoFailed) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.7f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Text(
                                                    text = "糟糕，找不到图片了...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "即将自动切换下一张",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.55f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "${cardIndex + 1} / ${groupGifs.size} 抽",
                                        style = MaterialTheme.typography.labelSmall.tabularNumbers,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentGif != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.md, vertical = 20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentGif.workTitle?.ifBlank { "沉浸动图套图" } ?: "沉浸动图套图",
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "groupTitleChange"
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!currentGif.authorNickname.isNullOrBlank()) {
                            AnimatedContent(
                                targetState = currentGif.authorNickname,
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "groupAuthorChange"
                            ) { nickname ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    // 触摸目标补到 48dp 下限，并合并语义让 TalkBack 作为单个按钮朗读
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .semantics(mergeDescendants = true) {
                                            role = Role.Button
                                        }
                                        .bouncyClickable(
                                            onClickLabel = stringResource(R.string.gif_view_author)
                                        ) { currentGif.authorId?.let { onAuthor(it) } }
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "@${nickname}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeIndex > 0) {
                            OutlinedButton(
                                onClick = { activeIndex -= 1 },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("上一张", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                if (activeIndex + 1 < groupGifs.size) activeIndex += 1
                                else onNextGroup()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (activeIndex + 1 < groupGifs.size) "下一张" else "下一套",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!currentGif.workId.isNullOrBlank()) {
                            Button(
                                onClick = { onDetail(currentGif.workId) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("看作品", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
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
    onShuffleClick: () -> Unit,
    shuffleLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.bouncyClickable(onClick = onBack)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }

            // 模式切换胶囊
            GifModeSegmentedPill(
                currentMode = currentMode,
                onModeChange = onSwitchMode
            )
        }

        // 换一组 / 换一个
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.bouncyClickable(onClick = onShuffleClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(text = shuffleLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSingle = currentMode == "single"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSingle) XhsRed else Color.Transparent,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .bouncyClickable { onModeChange("single") }
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "单张随机",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSingle) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSingle) Color.White else Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            val isGroup = currentMode == "group"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGroup) XhsRed else Color.Transparent,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .bouncyClickable { onModeChange("group") }
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "整组套图",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isGroup) FontWeight.Bold else FontWeight.Normal,
                        color = if (isGroup) Color.White else Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
