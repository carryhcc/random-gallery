package com.example.randomgallery.android.ui.piclist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.Downloader
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.MediaKind

@Composable
fun PicListScreen(
    viewModel: PicListViewModel,
    groupName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val gridState = rememberLazyStaggeredGridState()
    var columnCount by remember { mutableIntStateOf(2) }

    val decodedGroupName = remember(groupName) { android.net.Uri.decode(groupName) }

    // Lightbox / 全屏大图看图页码与状态 (null 时关闭)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // 距底部 3 条时触发自动加载下一页
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 3) viewModel.loadMore()
        }
    }

    val gridFullSizePx = with(LocalDensity.current) {
        val cellDp = (LocalConfiguration.current.screenWidthDp - Spacing.sm.value * (columnCount + 1)) / columnCount
        (cellDp * density * 2).toInt()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            XhsTopBar(
                title = decodedGroupName.ifBlank { stringResource(R.string.group_detail_fallback) },
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { columnCount = if (columnCount == 2) 3 else 2 }
                    ) {
                        Icon(
                            imageVector = if (columnCount == 2) Icons.Filled.GridOn else Icons.Filled.GridView,
                            contentDescription = "切换网格列数",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                items.isEmpty() && loading -> XhsLoadingBox(Modifier.fillMaxSize())
                items.isEmpty() && !loading ->
                    XhsEmptyState(
                        message = error ?: stringResource(R.string.piclist_empty),
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                else -> {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Fixed(columnCount),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalItemSpacing = Spacing.sm,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── 顶部套图名称与状态 Banner ─────────────────────
                        item(span = StaggeredGridItemSpan.FullLine) {
                            GroupHeroHeader(
                                groupTitle = decodedGroupName.ifBlank { stringResource(R.string.group_detail_fallback) },
                                loadedCount = items.size
                            )
                        }

                        // ── 图片瀑布流卡片 ─────────────────────────────
                        itemsIndexed(
                            items = items,
                            key = { index, pic -> pic.id ?: "idx_$index" }
                        ) { index, pic ->
                            StaggeredItemEntrance(index = index) {
                                PicTile(
                                    pic = pic,
                                    fullSizePx = gridFullSizePx,
                                    onClick = { selectedIndex = index },
                                    onDownload = { url ->
                                        Downloader.enqueue(context, url, MediaKind.IMAGE)
                                            .onSuccess { Messenger.show(context.getString(R.string.piclist_downloading)) }
                                            .onFailure { e -> Messenger.show(e.message ?: "下载失败", isError = true) }
                                    }
                                )
                            }
                        }

                        if (loading) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 参照随机图库调性强化的全屏看图器 (Horizontal Pager Lightbox) ─────
    if (selectedIndex != null && items.isNotEmpty()) {
        PicLightboxDialog(
            items = items,
            initialIndex = selectedIndex!!.coerceIn(0, items.size - 1),
            onDismiss = { selectedIndex = null },
            onDownload = { url ->
                Downloader.enqueue(context, url, MediaKind.IMAGE)
                    .onSuccess { Messenger.show(context.getString(R.string.piclist_downloading)) }
                    .onFailure { e -> Messenger.show(e.message ?: "下载失败", isError = true) }
            }
        )
    }
}

// ── 套图头部 Header Card ─────────────────────────────────────────────

@Composable
private fun GroupHeroHeader(
    groupTitle: String,
    loadedCount: Int
) {
    M3GlassCard(
        shape = RoundedCornerShape(20.dp),
        elevation = 1.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.xs)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp)
        ) {
            Text(
                text = groupTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "已加载 $loadedCount 张图片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── 图片网格 Tile 控件 ─────────────────────────────────────────────

@Composable
private fun PicTile(
    pic: PicVO,
    fullSizePx: Int,
    onClick: () -> Unit,
    onDownload: (String) -> Unit
) {
    val url = ImageUrlResolver.displayUrl(pic.picUrl) ?: ""
    var imageLoaded by remember(url) { mutableStateOf(false) }

    M3GlassSurface(
        shape = RoundedCornerShape(14.dp),
        elevation = 1.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .bouncyClickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SmartImage(
                url = url,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fullSize = fullSizePx,
                onFullLoaded = { imageLoaded = true },
                contentDescription = pic.picName
            )

            // 悬浮一键保存按钮
            if (imageLoaded && url.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onDownload(url) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "下载",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── 结合随机单图风格细节优化的全屏看图器 Dialog ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PicLightboxDialog(
    items: List<PicVO>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { items.size })
    val ratioCache = remember { mutableStateMapOf<String, Float>() }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 背景低饱和弥散渐变（与随机图片页一致）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            endY = 500f
                        )
                    )
            )

            // 左右滑动看图内容区
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val pic = items.getOrNull(page)
                val url = ImageUrlResolver.displayUrl(pic?.picUrl) ?: ""
                val rawRatio = if (url.isBlank()) 0.75f else ratioCache[url] ?: 0.75f

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.md, vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 居中圆角图片卡片，支持手势捏合双指缩放与双击放大
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .aspectRatio(rawRatio.coerceIn(0.55f, 1.4f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (url.isNotBlank()) {
                            ZoomableBox(
                                modifier = Modifier.fillMaxSize(),
                                onSingleTap = onDismiss
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = pic?.picName,
                                    contentScale = ContentScale.Crop,
                                    onSuccess = { state ->
                                        val size = state.painter.intrinsicSize
                                        if (size.width > 0f && size.height > 0f) {
                                            ratioCache[url] = size.width / size.height
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            XhsLoadingBox(Modifier.fillMaxSize())
                        }
                    }

                    if (!pic?.picName.isNullOrBlank()) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = pic?.picName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 顶部固定控制栏 (页码指示器 + 关闭按钮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3GlassChip(
                    text = "${pagerState.currentPage + 1} / ${items.size}",
                    icon = Icons.Filled.Image
                )

                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", modifier = Modifier.size(18.dp))
                }
            }

            // 底部悬浮控制栏 (保存按钮)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.Center
            ) {
                val currentPic = items.getOrNull(pagerState.currentPage)
                val currentUrl = ImageUrlResolver.displayUrl(currentPic?.picUrl) ?: ""

                Button(
                    onClick = {
                        if (currentUrl.isNotBlank()) onDownload(currentUrl)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("保存此原图", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}