package com.example.randomgallery.android.ui.gif

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PageState(
    val playerReady: Boolean? = true, // true=缓冲中, false=就绪, null=失败
    val videoRatio: Float = 3f / 4f
)

@OptIn(UnstableApi::class)
@Composable
fun RandomGifScreen(
    onBack: () -> Unit,
    onDetail: (workId: String) -> Unit,
    onAuthor: (authorId: String) -> Unit,
    viewModel: RandomGifViewModel
) {
    val context = LocalContext.current
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val gifList by viewModel.gifList.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // 两个 ExoPlayer 轮换：page 偶数用 players[0]，奇数用 players[1]
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
                    playWhenReady = false
                    volume = 0f
                }
        }
    }
    val playerUrls = remember { Array(2) { "" } }
    DisposableEffect(Unit) { onDispose { players.forEach { it.release() } } }

    fun playerFor(page: Int, settled: Int): ExoPlayer? =
        if (page == settled || page == settled + 1) players[page % 2] else null

    val pageStates = remember { mutableStateMapOf<Int, PageState>() }
    fun stateOf(page: Int) = pageStates[page] ?: PageState()

    val pagerState = rememberPagerState(pageCount = { gifList.size.coerceAtLeast(1) })
    val settledPage = pagerState.settledPage
    val preloadPage = settledPage + 1
    var retryTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // 监听两个 player 的事件
    DisposableEffect(settledPage) {
        val curPage = settledPage
        val nxtPage = preloadPage
        val curIdx = curPage % 2
        val preIdx = nxtPage % 2

        val curListener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                pageStates[curPage] = stateOf(curPage).copy(
                    playerReady = when (state) {
                        Player.STATE_READY -> false
                        Player.STATE_BUFFERING -> true
                        else -> stateOf(curPage).playerReady
                    }
                )
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                pageStates[curPage] = stateOf(curPage).copy(playerReady = null)
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0)
                    pageStates[curPage] = stateOf(curPage).copy(videoRatio = size.width.toFloat() / size.height)
            }
        }
        val preListener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                pageStates[nxtPage] = stateOf(nxtPage).copy(
                    playerReady = when (state) {
                        Player.STATE_READY -> false
                        Player.STATE_BUFFERING -> true
                        else -> stateOf(nxtPage).playerReady
                    }
                )
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
                pageStates[nxtPage] = stateOf(nxtPage).copy(playerReady = null)
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0)
                    pageStates[nxtPage] = stateOf(nxtPage).copy(videoRatio = size.width.toFloat() / size.height)
            }
        }
        players[curIdx].addListener(curListener)
        players[preIdx].addListener(preListener)
        onDispose {
            players[curIdx].removeListener(curListener)
            players[preIdx].removeListener(preListener)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            val curIdx = settledPage % 2
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                players.forEach { it.pause() }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (gifList.isNotEmpty()) {
                    players[curIdx].play()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 页面稳定后加载 & 预加载
    LaunchedEffect(settledPage, gifList.size, retryTick) {
        if (gifList.isEmpty()) return@LaunchedEffect
        if (settledPage >= gifList.size - 2) viewModel.loadNext()

        val url = gifList.getOrNull(settledPage)?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
            ?: return@LaunchedEffect
        val nextUrl = gifList.getOrNull(preloadPage)?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }

        val curIdx = settledPage % 2
        val preIdx = preloadPage % 2

        if (playerUrls[curIdx] != url) {
            pageStates[settledPage] = PageState(playerReady = true, videoRatio = 3f / 4f)
            players[curIdx].setMediaItem(MediaItem.fromUri(url))
            players[curIdx].prepare()
            playerUrls[curIdx] = url
        } else if (players[curIdx].playbackState == Player.STATE_READY) {
            pageStates[settledPage] = stateOf(settledPage).copy(playerReady = false)
        }
        // 暂停另一个 player
        players[preIdx].pause()
        players[curIdx].play()

        // 提前预加载下一页
        if (!nextUrl.isNullOrBlank() && playerUrls[preIdx] != nextUrl) {
            players[preIdx].setMediaItem(MediaItem.fromUri(nextUrl))
            players[preIdx].prepare()
            playerUrls[preIdx] = nextUrl
            pageStates[preloadPage] = PageState(playerReady = true, videoRatio = 3f / 4f)
        }
    }

    // 8 秒超时检测
    LaunchedEffect(settledPage, retryTick) {
        delay(8000)
        if (stateOf(settledPage).playerReady == true)
            pageStates[settledPage] = stateOf(settledPage).copy(playerReady = null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            loading && gifList.isEmpty() -> XhsLoadingBox(Modifier.fillMaxSize())
            error != null && gifList.isEmpty() -> XhsEmptyState(
                error ?: stringResource(R.string.common_load_failed),
                onRetry = { viewModel.loadNext() },
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp,
                    beyondViewportPageCount = 1
                ) { page ->
                    val gif = gifList.getOrNull(page)
                    val ps = stateOf(page)
                    val player = playerFor(page, settledPage)
                    val displayUrl = gif?.mediaUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── 1. 全屏高斯模糊动态色彩氛围背景 (弥散光晕彻底填补留白) ──
                        if (displayUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(displayUrl)
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

                        // ── 2. 自适应比例视口容器 (FIT + 弹性高度限制，不超出屏幕) ──
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Spacing.md, vertical = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(ps.videoRatio.coerceIn(0.55f, 1.8f))
                                    .clip(RoundedCornerShape(24.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (player != null) {
                                        AndroidView(
                                            factory = { ctx ->
                                                PlayerView(ctx).apply {
                                                    useController = false
                                                    controllerAutoShow = false
                                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                    layoutParams = ViewGroup.LayoutParams(
                                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                                        ViewGroup.LayoutParams.MATCH_PARENT
                                                    )
                                                    this.player = player
                                                }
                                            },
                                            update = { view ->
                                                view.useController = false
                                                view.hideController()
                                                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                if (view.player != player) view.player = player
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // 加载中指示
                                    if (page == settledPage && ps.playerReady == true) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(36.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }

                                    // 加载失败重试面板
                                    if (page == settledPage && ps.playerReady == null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.75f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.gif_load_failed),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            playerUrls[settledPage % 2] = ""
                                                            pageStates[settledPage] = PageState(playerReady = true, videoRatio = 3f / 4f)
                                                            retryTick++
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(stringResource(R.string.gif_retry))
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch {
                                                                val next = settledPage + 1
                                                                if (next < gifList.size) pagerState.animateScrollToPage(next)
                                                                else viewModel.loadNext()
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(stringResource(R.string.gif_skip), color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── 3. 底部半透明悬浮作品信息卡片 (小红书风格标题 + 作者 + 详情直达) ──
                        if (gif != null) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = Spacing.md, vertical = 24.dp)
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
                                        Text(
                                            text = gif.workTitle?.ifBlank { "沉浸动图短片" } ?: "沉浸动图短片",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!gif.authorNickname.isNullOrBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.clickable { gif.authorId?.let { onAuthor(it) } }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD54F),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "@${gif.authorNickname}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFFD54F),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (!gif.workId.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = XhsRed,
                                            modifier = Modifier.bouncyClickable { onDetail(gif.workId) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "看作品",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── 4. 顶部悬浮栏：返回 + 左上角胶囊模式切换 + 右上角切片 ──
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

                                // 模式切换胶囊 Tab：【单张随机 | 一套动图】
                                GifModeSegmentedPill(
                                    currentMode = playMode,
                                    onModeChange = { mode -> viewModel.switchMode(mode) }
                                )
                            }

                            // 换一张 / 换一套
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.bouncyClickable {
                                    scope.launch {
                                        val next = settledPage + 1
                                        if (next < gifList.size) pagerState.animateScrollToPage(next)
                                        else viewModel.loadNext()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (playMode == "group") "换一套" else "换一个",
                                        style = MaterialTheme.typography.labelSmall,
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
}

// ── 动图播放模式切换胶囊 Tab (单张随机 vs 一套动图) ─────────────────────

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
            // 单张模式
            val isSingle = currentMode == "single"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSingle) XhsRed else Color.Transparent,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .bouncyClickable { onModeChange("single") }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "单张随机",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSingle) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSingle) Color.White else Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // 套图模式
            val isGroup = currentMode == "group"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGroup) XhsRed else Color.Transparent,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .bouncyClickable { onModeChange("group") }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
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
