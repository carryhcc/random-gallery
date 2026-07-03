package com.example.randomgallery.android.ui.gif

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.randomgallery.android.ui.common.XhsEmptyState
import com.example.randomgallery.android.ui.common.XhsLoadingBox
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver

private data class PageState(
    val playerReady: Boolean? = true,
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
    val gifList by viewModel.gifList.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)
    val error by viewModel.error.observeAsState()

    // 两个 ExoPlayer 轮换：page 偶数用 players[0]，奇数用 players[1]
    // 这个映射是固定的，与任何 Compose 状态无关，不存在时序 gap
    val players = remember {
        Array(2) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                volume = 0f
            }
        }
    }
    val playerUrls = remember { Array(2) { "" } }
    DisposableEffect(Unit) { onDispose { players.forEach { it.release() } } }

    // page → players[page % 2]，在任何时刻都一致，拖动时与归位后返回同一实例
    fun playerFor(page: Int, settled: Int): ExoPlayer? =
        if (page == settled || page == settled + 1) players[page % 2] else null

    val pageStates = remember { mutableStateMapOf<Int, PageState>() }
    fun stateOf(page: Int) = pageStates[page] ?: PageState()

    val pagerState = rememberPagerState(pageCount = { gifList.size.coerceAtLeast(1) })
    val settledPage = pagerState.settledPage
    val preloadPage = settledPage + 1

    // 监听两个 player 的事件，分别更新对应 page 的状态
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

    // 页面稳定后加载 & 预加载
    LaunchedEffect(settledPage, gifList.size) {
        if (gifList.isEmpty()) return@LaunchedEffect
        if (settledPage >= gifList.size - 2) viewModel.loadNext()

        val url = gifList.getOrNull(settledPage)?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
            ?: return@LaunchedEffect
        val nextUrl = gifList.getOrNull(preloadPage)?.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }

        val curIdx = settledPage % 2
        val preIdx = preloadPage % 2

        // 当前页：若 player 已经有这个 url 就直接 play，不重置 pageState（避免闪烁）
        if (playerUrls[curIdx] != url) {
            pageStates[settledPage] = PageState(playerReady = true, videoRatio = 3f / 4f)
            players[curIdx].setMediaItem(MediaItem.fromUri(url))
            players[curIdx].prepare()
            playerUrls[curIdx] = url
        } else if (players[curIdx].playbackState == Player.STATE_READY) {
            pageStates[settledPage] = stateOf(settledPage).copy(playerReady = false)
        }
        // 暂停另一个 player（上一页）
        players[preIdx].pause()
        players[curIdx].play()

        // 预加载下一页（prepare but don't play）
        if (!nextUrl.isNullOrBlank() && playerUrls[preIdx] != nextUrl) {
            players[preIdx].setMediaItem(MediaItem.fromUri(nextUrl))
            players[preIdx].prepare()
            playerUrls[preIdx] = nextUrl
            pageStates[preloadPage] = PageState(playerReady = true, videoRatio = 3f / 4f)
        }
    }

    // 2 秒超时：仍在缓冲则标记失效并自动跳过
    LaunchedEffect(settledPage) {
        kotlinx.coroutines.delay(2000)
        if (stateOf(settledPage).playerReady == true)
            pageStates[settledPage] = stateOf(settledPage).copy(playerReady = null)
    }
    val curFailed = stateOf(settledPage).playerReady == null
    LaunchedEffect(curFailed, settledPage) {
        if (curFailed && gifList.isNotEmpty()) {
            val next = settledPage + 1
            if (next < gifList.size) pagerState.animateScrollToPage(next)
            else viewModel.loadNext()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            loading && gifList.isEmpty() -> XhsLoadingBox(Modifier.fillMaxSize())
            error != null && gifList.isEmpty() -> XhsEmptyState(
                error ?: "加载失败",
                onRetry = { viewModel.loadNext() },
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 12.dp,
                    beyondViewportPageCount = 1
                ) { page ->
                    val gif = gifList.getOrNull(page)
                    val ps = stateOf(page)
                    val animatedRatio by animateFloatAsState(ps.videoRatio, tween(250), label = "ratio$page")
                    // settled 在组合时捕获，与 playerFor 用同一个值
                    val player = playerFor(page, settledPage)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), Color.Transparent),
                                    endY = 500f
                                )
                            )
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(Modifier.height(48.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(animatedRatio)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
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

                            if (page == settledPage && ps.playerReady == true) {
                                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            if (page == settledPage && ps.playerReady == null) {
                                Box(
                                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
                                    Alignment.Center
                                ) {
                                    Text(
                                        "链接已失效，正在跳过…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (gif != null) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                gif.workTitle?.let { title ->
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .pointerInput(gif.workId) {
                                                detectTapGestures { gif.workId?.let { onDetail(it) } }
                                            }
                                    )
                                }
                                gif.authorNickname?.let { nickname ->
                                    Text(
                                        text = "@$nickname",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .pointerInput(gif.authorId) {
                                                detectTapGestures { gif.authorId?.let { onAuthor(it) } }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val total = gifList.size
                    val current = pagerState.currentPage
                    val start = (current - 3).coerceAtLeast(0)
                    val end = (start + 7).coerceAtMost(total)
                    repeat(end - start) { i ->
                        val isActive = (start + i) == current
                        Box(
                            Modifier
                                .size(if (isActive) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                    }
                }

                Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(Spacing.sm)) {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
