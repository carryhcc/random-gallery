package com.example.randomgallery.android.ui.downloaddetail

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import coil.size.Size
import com.example.randomgallery.android.data.model.XhsWorkMedia
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.Downloader
import com.example.randomgallery.android.util.MediaKind

private data class MediaItem2(
    val id: Long,
    val url: String,
    val isVideo: Boolean,
    val sortIndex: Int,
    val rawMedia: XhsWorkMedia
)

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailScreen(
    viewModel: DownloadDetailViewModel,
    workId: String,
    coverImageUrl: String = "",
    onBack: () -> Unit,
    onAuthorClick: (authorId: String, authorName: String) -> Unit = { _, _ -> },
    onTagClick: (tag: String) -> Unit = {}
) {
    val context = LocalContext.current
    val detailState by viewModel.detail.collectAsStateWithLifecycle()

    var showDeleteWorkDialog by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }
    var fullScreenUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(workId) { viewModel.load(workId) }
    LaunchedEffect(Unit) {
        viewModel.deleteWorkEvents.collect {
            it.onSuccess { onBack() }
                .onFailure { e -> Messenger.show(e.message ?: "删除失败", isError = true) }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.deleteMediaEvents.collect {
            it.onFailure { e -> Messenger.show(e.message ?: "删除失败", isError = true) }
        }
    }

    val detail = (detailState as? UiState.Success)?.data
    val base = detail?.baseInfo

    val imageMedia: List<MediaItem2> = remember(detail, coverImageUrl) {
        val sorted = (detail?.images ?: emptyList())
            .map { MediaItem2(it.id ?: 0, ImageUrlResolver.displayUrl(it.mediaUrl), false, it.sortIndex ?: 0, it) }
            .sortedBy { it.sortIndex }
        if (coverImageUrl.isBlank()) return@remember sorted
        val coverIdx = sorted.indexOfFirst { it.url == coverImageUrl }
        if (coverIdx <= 0) sorted
        else listOf(sorted[coverIdx]) + sorted.subList(0, coverIdx) + sorted.subList(coverIdx + 1, sorted.size)
    }
    val videoMedia: List<MediaItem2> = remember(detail) {
        (detail?.gifs ?: emptyList())
            .map { MediaItem2(it.id ?: 0, ImageUrlResolver.rawUrl(it.mediaUrl), true, it.sortIndex ?: 0, it) }
            .sortedBy { it.sortIndex }
    }

    val hasBothTabs = imageMedia.isNotEmpty() && videoMedia.isNotEmpty()
    var selectedTab by remember { mutableStateOf(0) }

    val imgPagerState = rememberPagerState(pageCount = { imageMedia.size.coerceAtLeast(1) })
    val vidPagerState = rememberPagerState(pageCount = { videoMedia.size.coerceAtLeast(1) })

    // ExoPlayer — 静音自动循环（实况图样式）
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = 0f
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val vidCurrentPage = vidPagerState.currentPage
    var videoError by remember { mutableStateOf(false) }
    var videoBuffering by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                videoError = true; videoBuffering = false
            }
            override fun onPlaybackStateChanged(state: Int) {
                videoBuffering = state == Player.STATE_BUFFERING
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(selectedTab, vidCurrentPage, videoMedia) {
        videoError = false
        videoBuffering = false   // 由 Player.Listener.onPlaybackStateChanged 在真正缓冲时置 true
        videoAspectRatio = null  // 切页重置，等待新视频上报
        if (selectedTab == 1 || (!hasBothTabs && videoMedia.isNotEmpty())) {
            val item = videoMedia.getOrNull(vidCurrentPage)
            if (item != null && item.url.isNotBlank()) {
                player.setMediaItem(MediaItem.fromUri(item.url))
                player.prepare()
                player.play()
            }
        } else {
            player.pause()
        }
    }

    val tags = remember(base?.workTags) {
        base?.workTags?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            XhsTopBar(
                title = base?.workTitle?.takeIf { it.isNotBlank() } ?: "",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteWorkDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除作品", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        when {
            detailState is UiState.Loading -> XhsLoadingBox(Modifier.fillMaxSize().padding(padding))
            detailState is UiState.Error -> XhsEmptyState(
                (detailState as UiState.Error).message,
                onRetry = { viewModel.load(workId) },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)
                ) {
                    // ── 媒体 Tab + Pager ──────────────────────────────
                    item {
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {

                            val showImages = !hasBothTabs && imageMedia.isNotEmpty() || hasBothTabs && selectedTab == 0
                            val showVideos = !hasBothTabs && videoMedia.isNotEmpty() || hasBothTabs && selectedTab == 1

                            if (showImages) {
                                MediaPagerBox(
                                    mediaList = imageMedia,
                                    pagerState = imgPagerState,
                                    onDelete = { deleteTargetId = it },
                                    onDownload = { url ->
                                        Downloader.enqueue(context, url, MediaKind.IMAGE)
                                        Messenger.show("图片正在下载，请稍候…")
                                    },
                                    onImageClick = { url -> fullScreenUrl = url }
                                ) { media, page ->
                                    val isCoverPage = page == 0 && coverImageUrl.isNotBlank() && media.url == coverImageUrl
                                    ImagePage(
                                        url = media.url,
                                        placeholderCacheKey = if (isCoverPage) coverImageUrl else null
                                    )
                                }
                            }

                            if (showVideos) {
                                MediaPagerBox(
                                    mediaList = videoMedia,
                                    pagerState = vidPagerState,
                                    onDelete = { deleteTargetId = it },
                                    onDownload = { url ->
                                        Downloader.enqueue(context, url, MediaKind.VIDEO)
                                        Messenger.show("视频正在下载，请稍候…")
                                    },
                                    onImageClick = null
                                ) { _, page ->
                                    LivePhotoPage(
                                        player = player,
                                        isCurrentPage = page == vidCurrentPage,
                                        isBuffering = videoBuffering && page == vidCurrentPage,
                                        hasError = videoError && page == vidCurrentPage,
                                        aspectRatio = if (page == vidCurrentPage) videoAspectRatio else null
                                    )
                                }
                            }

                            if (imageMedia.isEmpty() && videoMedia.isEmpty()) {
                                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                                    Text("暂无媒体", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // ── 图片/实况 切换 pill（在 pager 下方）──────────────
                            if (hasBothTabs) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MediaTabPill(
                                        label = "图片",
                                        count = imageMedia.size,
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 }
                                    )
                                    Spacer(Modifier.width(Spacing.sm))
                                    MediaTabPill(
                                        label = "实况",
                                        count = videoMedia.size,
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 }
                                    )
                                }
                            }

                            } // end Column
                        } // end Surface
                    }

                    // ── 作品信息卡 ──────────────────────────────────
                    item {
                        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {

                                base?.workTitle?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }

                                base?.authorNickname?.let { name ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { base.authorId?.let { onAuthorClick(it, name) } }
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                                    ) {
                                        Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (tags.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                        items(tags) { tag ->
                                            Box(
                                                Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { onTagClick(tag) }
                                                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                                            ) {
                                                Text(tag, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                base?.publishTime?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                        Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.xhs.textTertiary, modifier = Modifier.size(13.dp))
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.xhs.textTertiary)
                                    }
                                }

                                base?.workDescription?.takeIf { it.isNotBlank() }?.let { desc ->
                                    FormattedDescription(text = desc, maxLines = 6, topicColor = MaterialTheme.xhs.topicLink)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                    base?.likeCount?.let { StatBadge(Icons.Filled.Favorite, it, MaterialTheme.xhs.accentCoral) }
                                    base?.collectCount?.let { StatBadge(Icons.Filled.Bookmark, it, MaterialTheme.xhs.accentOrange) }
                                    base?.commentCount?.let { StatBadge(Icons.Filled.ChatBubble, it, MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(Spacing.xl))
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }

    // ── 全屏大图查看 ──────────────────────────────────────────────────
    fullScreenUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullScreenUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenUrl = null }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                // 关闭提示
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.lg)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    deleteTargetId?.let { mediaId ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("删除媒体") },
            text = { Text("删除这个媒体文件？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMedia(mediaId, workId); deleteTargetId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTargetId = null }) { Text("取消") } }
        )
    }

    if (showDeleteWorkDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWorkDialog = false },
            title = { Text("删除作品") },
            text = { Text("确定要删除整个作品及其所有媒体吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteWork(workId); showDeleteWorkDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteWorkDialog = false }) { Text("取消") } }
        )
    }
}

// ── 通用翻页容器 ──────────────────────────────────────────────────────

@Composable
private fun MediaPagerBox(
    mediaList: List<MediaItem2>,
    pagerState: PagerState,
    onDelete: (Long) -> Unit,
    onDownload: (String) -> Unit,
    onImageClick: ((String) -> Unit)?,
    pageContent: @Composable BoxScope.(media: MediaItem2, page: Int) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = Spacing.md)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) { page ->
            val media = mediaList[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (onImageClick != null) Modifier.clickable { onImageClick(media.url) }
                        else Modifier
                    )
            ) {
                pageContent(media, page)

                // 右上角操作按钮行
                Row(
                    Modifier.align(Alignment.TopEnd).padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // 下载按钮
                    MediaOverlayButton(icon = Icons.Filled.FileDownload, desc = "下载") {
                        onDownload(media.url)
                    }
                    // 删除按钮
                    MediaOverlayButton(icon = Icons.Filled.Delete, desc = "删除") {
                        onDelete(media.id)
                    }
                }
            }
        }

        // 页码指示器
        PageIndicator(
            total = mediaList.size,
            current = pagerState.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun MediaOverlayButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, desc, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

// ── 图片页 ────────────────────────────────────────────────────────────

// 图片加载状态：Loading / Success(painter) / Error
private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Success(val painter: androidx.compose.ui.graphics.painter.Painter) : ImageLoadState
    data object Error : ImageLoadState
}

@Composable
private fun BoxScope.ImagePage(url: String, placeholderCacheKey: String? = null) {
    val context = LocalContext.current
    val request = remember(url, placeholderCacheKey) {
        ImageRequest.Builder(context).data(url)
            .size(Size.ORIGINAL)
            .apply { if (placeholderCacheKey != null) placeholderMemoryCacheKey(placeholderCacheKey) }
            .build()
    }

    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    var loadState by remember(url) { mutableStateOf<ImageLoadState>(ImageLoadState.Loading) }
    // 加载前用 3:4 占位，加载完成后过渡到图片真实比例
    var targetRatio by remember(url) { mutableStateOf(3f / 4f) }
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 350),
        label = "img_ratio"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(animatedRatio)
            .background(bgColor)
    ) {
        Crossfade(
            targetState = loadState,
            animationSpec = tween(durationMillis = 300),
            label = "img_crossfade"
        ) { state ->
            when (state) {
                ImageLoadState.Loading -> MediaShimmer(dark = false)
                ImageLoadState.Error   -> BrokenPlaceholder(isVideo = false)
                is ImageLoadState.Success -> androidx.compose.foundation.Image(
                    painter = state.painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.size(1.dp),
            onSuccess = {
                val w = it.painter.intrinsicSize.width
                val h = it.painter.intrinsicSize.height
                if (w > 0 && h > 0 && !w.isNaN() && !h.isNaN()) targetRatio = w / h
                loadState = ImageLoadState.Success(it.painter)
            },
            onError   = { loadState = ImageLoadState.Error },
            onLoading = { if (loadState !is ImageLoadState.Success) loadState = ImageLoadState.Loading }
        )
    }
}

// ── 实况图（视频）页 ──────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun BoxScope.LivePhotoPage(
    player: ExoPlayer,
    isCurrentPage: Boolean,
    isBuffering: Boolean = false,
    hasError: Boolean = false,
    aspectRatio: Float? = null
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    // 加载前用 9:16 占位，视频尺寸上报后过渡到真实比例
    val animatedRatio by animateFloatAsState(
        targetValue = aspectRatio ?: (9f / 16f),
        animationSpec = tween(durationMillis = 350),
        label = "vid_ratio"
    )
    val sizeMod = Modifier
        .fillMaxWidth()
        .aspectRatio(animatedRatio)
        .background(bgColor)

    when {
        hasError -> Box(sizeMod) { BrokenPlaceholder(isVideo = true) }

        isCurrentPage -> Box(sizeMod) {
            // PlayerView 始终存在，Crossfade 控制 shimmer 叠层的淡出
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
            // 缓冲时叠加 shimmer，准备好后淡出
            Crossfade(
                targetState = isBuffering,
                animationSpec = tween(300),
                label = "vid_shimmer"
            ) { buffering ->
                if (buffering) MediaShimmer(dark = true)
            }
            // "实况" 角标
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.sm)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("实况", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        else -> Box(sizeMod, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(52.dp))
        }
    }
}
// ── Shimmer 特效 ──────────────────────────────────────────────────────

@Composable
private fun BoxScope.MediaShimmer(dark: Boolean) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer_x"
    )
    val colors = if (dark) listOf(Color(0xFF1C1C1C), Color(0xFF2E2E2E), Color(0xFF3A3A3A), Color(0xFF2E2E2E), Color(0xFF1C1C1C))
    else listOf(Color(0xFFF0F0F0), Color(0xFFE4E4E4), Color(0xFFD8D8D8), Color(0xFFE4E4E4), Color(0xFFF0F0F0))
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(colors, start = Offset(offset * 800f, 0f), end = Offset((offset + 1f) * 800f, 400f))
        )
    )
}

// ── 失败占位 ──────────────────────────────────────────────────────────

@Composable
private fun BrokenPlaceholder(isVideo: Boolean) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(
                if (isVideo) Icons.Filled.VideocamOff else Icons.Filled.BrokenImage,
                null, tint = MaterialTheme.xhs.textTertiary, modifier = Modifier.size(44.dp)
            )
            Text(if (isVideo) "视频已过期" else "图片已过期", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.xhs.textTertiary)
        }
    }
}

// ── 页码指示器 ────────────────────────────────────────────────────────

@Composable
private fun PageIndicator(total: Int, current: Int, modifier: Modifier = Modifier) {
    if (total <= 1) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        if (total <= 9) {
            repeat(total) { i ->
                Box(
                    Modifier.size(if (i == current) 7.dp else 5.dp).clip(CircleShape)
                        .background(if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
            }
        } else {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.outline)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
            ) {
                Text("${current + 1} / $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── 富文本描述（话题高亮）────────────────────────────────────────────

@Composable
private fun FormattedDescription(text: String, maxLines: Int = Int.MAX_VALUE, topicColor: Color) {
    // 匹配 #任意文字[话题]# 或 #任意文字# 两种格式
    val topicRegex = Regex("""#([^#]+?)(?:\[话题])?#""")
    val annotatedString = remember(text, topicColor) {
        buildAnnotatedString {
            var cursor = 0
            for (match in topicRegex.findAll(text)) {
                // 普通文字
                if (match.range.first > cursor) {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Unspecified)) {
                        append(text.substring(cursor, match.range.first))
                    }
                }
                // 话题高亮
                withStyle(androidx.compose.ui.text.SpanStyle(color = topicColor, fontWeight = FontWeight.Medium)) {
                    append("#${match.groupValues[1]}")
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

// ── 媒体类型切换 Pill ────────────────────────────────────────────────

@Composable
private fun MediaTabPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(150),
        label = "pill_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "pill_text"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label  $count",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ── 统计角标 ──────────────────────────────────────────────────────────

@Composable
private fun StatBadge(icon: ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
