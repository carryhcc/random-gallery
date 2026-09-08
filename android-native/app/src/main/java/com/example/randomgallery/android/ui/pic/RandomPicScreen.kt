package com.example.randomgallery.android.ui.pic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import com.example.randomgallery.android.ui.common.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.Downloader
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.MediaKind
import kotlinx.coroutines.launch

/** 随机一图：对齐随机动图 UI 架构，主题清爽背景，居中圆角卡片，流畅左右滑动切图 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPicScreen(
    viewModel: RandomPicViewModel,
    onBack: () -> Unit,
    onGroupClick: (groupId: Long, groupName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val picList by viewModel.picList.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val ratioCache = remember { mutableStateMapOf<String, Float>() }

    val pagerState = rememberPagerState(pageCount = { picList.size.coerceAtLeast(1) })
    val settledPage = pagerState.settledPage

    // 浮窗大图预览 URL
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var pendingScrollToNext by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (picList.isEmpty()) viewModel.loadRandomPic()
    }

    // 当图片列表追加新图且处于待翻页状态时，平滑翻到新图
    LaunchedEffect(picList.size) {
        if (pendingScrollToNext && picList.isNotEmpty()) {
            pendingScrollToNext = false
            pagerState.animateScrollToPage(picList.size - 1)
        }
    }

    // 滑动到倒数第2页时自动预加载下一张随机图
    LaunchedEffect(settledPage, picList.size) {
        if (picList.isNotEmpty() && settledPage >= picList.size - 2) {
            viewModel.loadNext()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_random_pic), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < picList.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                pendingScrollToNext = true
                                viewModel.loadNext()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.pic_change))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                loading && picList.isEmpty() -> XhsLoadingBox(Modifier.fillMaxSize())
                error != null && picList.isEmpty() ->
                    XhsEmptyState(
                        error ?: stringResource(R.string.common_load_failed),
                        onRetry = { viewModel.loadRandomPic() },
                        modifier = Modifier.fillMaxSize()
                    )
                else -> {
                    // 1. 左右滑动切图主视图 (HorizontalPager)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 12.dp,
                        beyondViewportPageCount = 1
                    ) { page ->
                        val pic = picList.getOrNull(page)
                        val url = pic?.picUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""
                        val rawRatio = if (url.isBlank()) 0.75f else ratioCache[url] ?: 0.75f
                        val animatedRatio by animateFloatAsState(rawRatio, tween(250), label = "ratio$page")

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
                                .padding(horizontal = Spacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 居中 20.dp 圆角图片卡片（点击放大浮窗预览，完全不阻碍左右滑动手势）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(animatedRatio)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .bouncyClickable(onClick = { if (url.isNotBlank()) previewUrl = url })
                            ) {
                                if (url.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = pic?.picName ?: stringResource(R.string.pic_title_fallback),
                                        contentScale = ContentScale.Crop,
                                        onSuccess = { state ->
                                            val size = state.painter.intrinsicSize
                                            if (url.isNotBlank() && size.width > 0f && size.height > 0f) {
                                                ratioCache[url] = (size.width / size.height).coerceIn(0.55f, 1.4f)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    XhsLoadingBox(Modifier.fillMaxSize())
                                }
                            }

                            Spacer(Modifier.height(Spacing.md))

                            // 图片名称与操作提示
                            if (pic != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.sm),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = pic.picName ?: stringResource(R.string.pic_title_fallback),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.height(60.dp))
                        }
                    }

                    // 2. 底部动态小圆点指示器（对齐随机动图）
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val total = picList.size
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

                    // 3. 底部悬浮操作栏（看同组套图胶囊 & 下载 FAB）
                    val currentPic = picList.getOrNull(settledPage)
                    val currentUrl = currentPic?.picUrl?.let { ImageUrlResolver.displayUrl(it) } ?: ""

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xl)
                    ) {
                        GlassSurface(
                            shape = CircleShape,
                            elevation = 10.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (currentPic?.groupId != null) {
                                    val gid = currentPic.groupId
                                    val fallbackName = stringResource(R.string.group_detail_fallback)
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                val name = viewModel.resolveGroupName(gid)
                                                onGroupClick(gid, name ?: fallbackName)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.FormatListBulleted,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.pic_view_group),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (currentUrl.isNotBlank()) {
                                            Downloader.enqueue(context, currentUrl, MediaKind.IMAGE)
                                                .onSuccess { Messenger.show(context.getString(R.string.pic_download_queued)) }
                                                .onFailure { e -> Messenger.show(e.message ?: "下载失败", isError = true) }
                                        }
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = stringResource(R.string.common_download),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 点击大图弹窗全屏预览 (Full Preview Dialog with Gestures)
    if (!previewUrl.isNullOrBlank()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { previewUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                ZoomableBox(
                    modifier = Modifier.fillMaxSize(),
                    onSingleTap = { previewUrl = null }
                ) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 顶部浮动关闭按钮
                IconButton(
                    onClick = { previewUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(Spacing.md),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
