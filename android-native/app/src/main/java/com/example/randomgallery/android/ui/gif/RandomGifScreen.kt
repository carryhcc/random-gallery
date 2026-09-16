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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (playMode == "group") {
            // ── 模式 B：【整组套图】扑克牌多层重叠物理抽牌视口 ────────────
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
            // ── 模式 A：【单张随机】仅中间卡片切换，背景与文本动态平滑流转 ──
            SingleCardDynamicGifViewer(
                gifList = singleGifs,
                loading = loading,
                error = error,
                onBack = onBack,
                onDetail = onDetail,
                onAuthor = onAuthor,
                onLoadNext = { viewModel.loadNext() },
                onSwitchMode = { viewModel.switchMode(it) }
            )
        }
    }
}

// ── 模式 A：单张随机（仅中间卡片手势切换，全屏背景高斯模糊与底部文案动态过渡）──

@OptIn(UnstableApi::class)
@Composable
private fun SingleCardDynamicGifViewer(
    gifList: List<RandomGifVO>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onDetail: (workId: String) -> Unit,
    onAuthor: (authorId: String) -> Unit,
    onLoadNext: () -> Unit,
    onSwitchMode: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()

    var activeIndex by remember { mutableIntStateOf(0) }
    var pendingAdvance by remember { mutableStateOf(false) }
    var isVideoReady by remember(activeIndex) { mutableStateOf(false) }
    var isVideoFailed by remember(activeIndex) { mutableStateOf(false) }
    var isCoverFailed by remember(activeIndex) { mutableStateOf(false) }
    var videoRatio by remember(activeIndex) { mutableFloatStateOf(0.75f) }

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

    // 中间单张卡片平移手势
    val cardOffsetX = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(0f) }

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
                        .padding(horizontal = 24.dp, vertical = 68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dragRotation = (cardOffsetX.value / screenWidthPx) * 16f

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoRatio)
                            .graphicsLayer {
                                translationX = cardOffsetX.value
                                translationY = cardOffsetY.value
                                rotationZ = dragRotation
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .pointerInput(activeIndex) {
                                detectDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            // 1. 向左划飞：切换到下一张
                                            if (cardOffsetX.value < -screenWidthPx * 0.22f) {
                                                cardOffsetX.animateTo(-screenWidthPx * 1.3f, spring(stiffness = Spring.StiffnessMediumLow))
                                                cardOffsetX.snapTo(0f)
                                                cardOffsetY.snapTo(0f)
                                                if (activeIndex + 1 < gifList.size) {
                                                    activeIndex += 1
                                                } else {
                                                    onLoadNext()
                                                }
                                            }
                                            // 2. 向右划回：退回到上一张
                                            else if (cardOffsetX.value > screenWidthPx * 0.22f) {
                                                if (activeIndex > 0) {
                                                    cardOffsetX.animateTo(screenWidthPx * 1.3f, spring(stiffness = Spring.StiffnessMediumLow))
                                                    cardOffsetX.snapTo(0f)
                                                    cardOffsetY.snapTo(0f)
                                                    activeIndex -= 1
                                                } else {
                                                    cardOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                    cardOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                }
                                            }
                                            // 3. 未过阈值复位
                                            else {
                                                cardOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                cardOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            cardOffsetX.snapTo(cardOffsetX.value + dragAmount.x)
                                            cardOffsetY.snapTo(cardOffsetY.value + dragAmount.y * 0.25f)
                                        }
                                    }
                                )
                            }
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
                        }
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
