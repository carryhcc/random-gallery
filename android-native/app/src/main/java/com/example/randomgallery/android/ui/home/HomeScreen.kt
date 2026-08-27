package com.example.randomgallery.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onWorkClick: (XhsWorkListVO) -> Unit,
    onNavigateToDownloadDetail: (workId: String, coverImageUrl: String) -> Unit,
    onNavigateToRandomPic: () -> Unit,
    onNavigateToRandomGif: () -> Unit,
    onNavigateToDownloadManage: () -> Unit,
    onNavigateToRandomGallery: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToDownloadList: () -> Unit,
    onNavigateToPicList: (groupId: Long, groupName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val spaceMode by viewModel.spaceMode.collectAsStateWithLifecycle()
    val envInfo by viewModel.envInfo.collectAsStateWithLifecycle()
    val heroWorks by viewModel.heroWorks.collectAsStateWithLifecycle()
    val heroLoading by viewModel.heroLoading.collectAsStateWithLifecycle()
    val feedWorks by viewModel.feedWorks.collectAsStateWithLifecycle()
    val feedLoading by viewModel.feedLoading.collectAsStateWithLifecycle()

    val galleryGroups by viewModel.galleryGroups.collectAsStateWithLifecycle()
    val galleryLoading by viewModel.galleryLoading.collectAsStateWithLifecycle()

    val privacy by viewModel.privacy.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val proxyEnabled by viewModel.proxyEnabled.collectAsStateWithLifecycle()
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()

    val localEnv by viewModel.localEnv.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val urlList by viewModel.urlList.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    val gridState = rememberLazyStaggeredGridState()
    val ratioCache = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            Messenger.show(msg, isError = msg.contains("失败"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.randomGroupEvents.collect { result ->
            result.onSuccess { group ->
                group.groupId?.let { gid ->
                    onNavigateToPicList(gid, group.groupName ?: context.getString(R.string.group_detail_fallback))
                }
            }.onFailure { err ->
                Messenger.show(err.message ?: context.getString(R.string.submit_failed_unknown), isError = true)
            }
        }
    }

    // 瀑布流触底自动加载更多
    LaunchedEffect(gridState, spaceMode) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastIndex to info.totalItemsCount
        }.collect { (lastIndex, total) ->
            if (total > 0 && lastIndex >= total - 4) {
                if (spaceMode == "gallery") viewModel.loadGalleryGroups()
                else viewModel.loadFeed()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    // 顶层双模态切换胶囊 (Segmented Capsule)
                    DualSpaceSegmentedPill(
                        currentMode = spaceMode,
                        onModeSelected = { mode -> viewModel.setSpaceMode(mode) }
                    )
                },
                actions = {
                    if (spaceMode == "gallery" && localEnv.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = localEnv.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (spaceMode == "gallery") viewModel.refreshGallery()
                            else viewModel.refreshFeed()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.common_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.common_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        val isRefreshing = if (spaceMode == "gallery") galleryLoading && galleryGroups.isNotEmpty()
                           else (feedLoading || heroLoading) && feedWorks.isNotEmpty()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (spaceMode == "gallery") viewModel.refreshGallery()
                else viewModel.refreshFeed()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (spaceMode == "gallery") {
                // ── 模式 B：【本地·图库】专属内容流 ───────────────────────
                val picCount = (envInfo as? UiState.Success)?.data?.picCount ?: 0L
                val groupCount = (envInfo as? UiState.Success)?.data?.groupCount ?: 0L

                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(minSize = 165.dp),
                    contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 80.dp),
                    verticalItemSpacing = Spacing.md,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        GallerySpaceHeroDashboard(
                            currentEnv = localEnv,
                            picCount = picCount,
                            groupCount = groupCount,
                            onSwitchEnv = { viewModel.switchEnv(it) },
                            onExploreGallery = onNavigateToRandomGallery
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        GalleryFeatureQuickBar(
                            onNavigateToRandomPic = onNavigateToRandomPic,
                            onNavigateToRandomGallery = onNavigateToRandomGallery,
                            onRandomGroup = { viewModel.randomGroup() },
                            onNavigateToGroupList = onNavigateToGroupList
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm, bottom = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "套图分组目录",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "多环境套图",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (galleryGroups.isEmpty() && galleryLoading) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = galleryGroups,
                            key = { index, item -> item.groupId ?: -index.toLong() }
                        ) { index, group ->
                            StaggeredItemEntrance(index = index) {
                                HomeGalleryGroupCard(
                                    group = group,
                                    ratioCache = ratioCache,
                                    onClick = {
                                        val gid = group.groupId ?: 0L
                                        if (gid > 0L) onNavigateToPicList(gid, group.groupName ?: context.getString(R.string.group_detail_fallback))
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ── 模式 A：【探索·精选】专属内容流 ───────────────────────
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(minSize = 165.dp),
                    contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 80.dp),
                    verticalItemSpacing = Spacing.md,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        ImmersiveHeroCarousel(
                            works = heroWorks,
                            isLoading = heroLoading,
                            onWorkClick = { work ->
                                work.workId?.let { workId ->
                                    onNavigateToDownloadDetail(workId, work.coverImageUrl ?: "")
                                }
                            },
                            onShuffleClick = { viewModel.loadHeroWorks(force = true) }
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        ExploreFeatureQuickBar(
                            onNavigateToRandomGif = onNavigateToRandomGif,
                            onNavigateToDownloadList = onNavigateToDownloadList,
                            onNavigateToDownloadManage = onNavigateToDownloadManage
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm, bottom = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Explore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "精选作品探索",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "小红书解析内容",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (feedWorks.isEmpty() && feedLoading) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = feedWorks,
                            key = { index, item -> item.workId ?: item.id ?: index.toLong() }
                        ) { index, work ->
                            StaggeredItemEntrance(index = index) {
                                HomeWorkFeedCard(
                                    work = work,
                                    ratioCache = ratioCache,
                                    onClick = {
                                        onWorkClick(work)
                                        work.workId?.let { workId ->
                                            onNavigateToDownloadDetail(workId, work.coverImageUrl ?: "")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsBottomSheet(
            currentUrl = baseUrl,
            urlList = urlList,
            privacyEnabled = privacy,
            currentDarkMode = darkMode,
            currentSpaceMode = spaceMode,
            proxyEnabled = proxyEnabled,
            proxyType = proxyType,
            proxyHost = proxyHost,
            proxyPort = proxyPort,
            onDismiss = { showSettings = false },
            onSelectUrl = { viewModel.selectBaseUrl(it) },
            onAddUrl = { viewModel.addAndSelectUrl(it) },
            onRemoveUrl = { viewModel.removeUrl(it) },
            onPrivacyToggle = { viewModel.setPrivacy(it) },
            onDarkModeSelect = { viewModel.setDarkMode(it) },
            onSaveProxyConfig = { enabled, type, host, port ->
                viewModel.saveProxyConfig(enabled, type, host, port)
            }
        )
    }
}

// ── 沉浸式 Hero 下载列表轮播展位 ─────────────────────────────────────

@Composable
private fun ImmersiveHeroCarousel(
    works: List<XhsWorkListVO>,
    isLoading: Boolean,
    onWorkClick: (XhsWorkListVO) -> Unit,
    onShuffleClick: () -> Unit
) {
    val context = LocalContext.current
    if (works.isEmpty() && isLoading) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        return
    }

    val displayWorks = if (works.isEmpty()) listOf(XhsWorkListVO(workTitle = "沉浸光影精选")) else works
    val pagerState = rememberPagerState(pageCount = { displayWorks.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
    ) {
        val currentWork = displayWorks.getOrNull(pagerState.currentPage)
        val rawUrl = currentWork?.coverImageUrl
        val displayUrl = ImageUrlResolver.displayUrl(rawUrl) ?: ""

        // 背景弥散光晕层
        if (displayUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .blur(28.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .graphicsLayer(alpha = 0.5f)
            )
        }

        // Hero 主卡片 HorizontalPager 左右轮播
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val work = displayWorks.getOrNull(page)
                val itemUrl = ImageUrlResolver.displayUrl(work?.coverImageUrl) ?: ""

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .bouncyClickable {
                            if (work != null && !work.workId.isNullOrBlank()) onWorkClick(work)
                        }
                ) {
                    // 封面图片
                    if (itemUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(itemUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = work?.workTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // 底部暗色渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.05f),
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.85f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )

                    // 顶部胶囊标签：精选推荐 & 轮播指示器
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "精选作品",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // 轮播页码指示胶囊
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "${page + 1} / ${displayWorks.size}",
                                style = MaterialTheme.typography.labelSmall.tabularNumbers,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 底部信息：作品标题 + 作者 / 图片张数
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = work?.workTitle?.ifBlank { "未命名作品" } ?: "沉浸光影精选",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!work?.authorNickname.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = work?.authorNickname ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            val imgCount = work?.imageCount ?: 0
                            if (imgCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "$imgCount 张",
                                        style = MaterialTheme.typography.labelSmall.tabularNumbers,
                                        color = Color.White.copy(alpha = 0.9f)
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

// ── 顶层双模态切换胶囊 (Dual Space Segmented Pill) ─────────────────────

@Composable
private fun DualSpaceSegmentedPill(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 模式 A：探索·精选 (XHS)
            val isExplore = currentMode != "gallery"
            val exploreBg = if (isExplore) MaterialTheme.colorScheme.primary else Color.Transparent
            val exploreContent = if (isExplore) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = exploreBg,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .bouncyClickable { onModeSelected("explore") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = null,
                        tint = exploreContent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "探索·精选",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isExplore) FontWeight.Bold else FontWeight.Medium,
                        color = exploreContent
                    )
                }
            }

            // 模式 B：本地·图库 (Environment Gallery)
            val isGallery = currentMode == "gallery"
            val galleryBg = if (isGallery) MaterialTheme.colorScheme.primary else Color.Transparent
            val galleryContent = if (isGallery) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = galleryBg,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .bouncyClickable { onModeSelected("gallery") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = galleryContent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "本地·图库",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isGallery) FontWeight.Bold else FontWeight.Medium,
                        color = galleryContent
                    )
                }
            }
        }
    }
}

// ── 探索模式快捷操作栏 ───────────────────────────────────────────────

@Composable
private fun ExploreFeatureQuickBar(
    onNavigateToRandomGif: () -> Unit,
    onNavigateToDownloadList: () -> Unit,
    onNavigateToDownloadManage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        VisualFeatureHeroCard(
            title = "动图短片",
            subtitle = "精彩动画",
            icon = Icons.Filled.Animation,
            gradient = Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
            badge = "动态",
            modifier = Modifier.weight(1f),
            onClick = onNavigateToRandomGif
        )

        VisualFeatureHeroCard(
            title = "作品列表",
            subtitle = "全部已下载",
            icon = Icons.Filled.PhotoAlbum,
            gradient = Brush.linearGradient(listOf(Color(0xFF4A90D9), Color(0xFF6B7FD7))),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToDownloadList
        )

        VisualFeatureHeroCard(
            title = "下载管理",
            subtitle = "解析队列",
            icon = Icons.Filled.Download,
            gradient = Brush.linearGradient(listOf(Color(0xFFFF8008), Color(0xFFFFC837))),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToDownloadManage
        )
    }
}

// ── 图库模式 Hero 看板与环境切换 ──────────────────────────────────────

@Composable
private fun GallerySpaceHeroDashboard(
    currentEnv: String,
    picCount: Long,
    groupCount: Long,
    onSwitchEnv: (String) -> Unit,
    onExploreGallery: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "环境数据库概览",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 快捷切环境 Chip
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("dev", "test", "prod").forEach { envKey ->
                        val selected = currentEnv.equals(envKey, ignoreCase = true)
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.bouncyClickable { onSwitchEnv(envKey) }
                        ) {
                            Text(
                                text = envKey.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 统计指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .bouncyClickable(onClick = onExploreGallery)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = if (picCount > 0) "$picCount" else "--",
                                style = MaterialTheme.typography.titleMedium.tabularNumbers,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "图片总数",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text(
                                text = if (groupCount > 0) "$groupCount" else "--",
                                style = MaterialTheme.typography.titleMedium.tabularNumbers,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "套图总组数",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 图库模式快捷操作栏 ───────────────────────────────────────────────

@Composable
private fun GalleryFeatureQuickBar(
    onNavigateToRandomPic: () -> Unit,
    onNavigateToRandomGallery: () -> Unit,
    onRandomGroup: () -> Unit,
    onNavigateToGroupList: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        VisualFeatureHeroCard(
            title = stringResource(R.string.home_random_gallery),
            subtitle = "画廊瀑布流",
            icon = Icons.Filled.GridView,
            gradient = Brush.linearGradient(listOf(Color(0xFFFF5E62), Color(0xFFFF9966))),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToRandomGallery
        )

        VisualFeatureHeroCard(
            title = stringResource(R.string.home_random_pic),
            subtitle = "单张看图",
            icon = Icons.Filled.Shuffle,
            gradient = Brush.linearGradient(listOf(Color(0xFF6B7FD7), Color(0xFF8E9EFA))),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToRandomPic
        )

        VisualFeatureHeroCard(
            title = stringResource(R.string.home_random_group),
            subtitle = "随机抽选",
            icon = Icons.Filled.Collections,
            gradient = Brush.linearGradient(listOf(Color(0xFF3BAD7A), Color(0xFF68D8A0))),
            modifier = Modifier.weight(1f),
            onClick = onRandomGroup
        )
    }
}

// ── 图库模式瀑布流卡片 ───────────────────────────────────────────────

@Composable
private fun HomeGalleryGroupCard(
    group: GroupVO,
    ratioCache: SnapshotStateMap<String, Float>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val url = ImageUrlResolver.displayUrl(group.groupUrl) ?: ""
    val ratio = if (url.isBlank()) 0.85f else ratioCache[url] ?: 0.85f

    M3GlassCard(
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = group.groupName,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val size = state.painter.intrinsicSize
                        if (url.isNotBlank() && size.width > 0f && size.height > 0f) {
                            ratioCache[url] = (size.width / size.height).coerceIn(0.65f, 1.4f)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                val count = group.groupCount ?: 0
                if (count > 0) {
                    M3GlassChip(
                        text = "$count 张",
                        icon = Icons.Filled.Image,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Text(
                text = group.groupName ?: stringResource(R.string.group_unnamed),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
    }
}

@Composable
private fun VisualFeatureHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .bouncyClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(12.dp)
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VisualFeatureCompactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val finalBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else bgColor

    M3GlassSurface(
        shape = RoundedCornerShape(16.dp),
        containerColor = finalBgColor,
        elevation = 1.dp,
        modifier = modifier
            .height(84.dp)
            .bouncyClickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (isDark) 0.2f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

// ── 首页探索瀑布流卡片（下载列表作品） ──────────────────────────────

@Composable
private fun HomeWorkFeedCard(
    work: XhsWorkListVO,
    ratioCache: SnapshotStateMap<String, Float>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val url = ImageUrlResolver.displayUrl(work.coverImageUrl) ?: ""
    val ratio = if (url.isBlank()) 0.85f else ratioCache[url] ?: 0.85f

    M3GlassCard(
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = work.workTitle,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val size = state.painter.intrinsicSize
                        if (url.isNotBlank() && size.width > 0f && size.height > 0f) {
                            ratioCache[url] = (size.width / size.height).coerceIn(0.65f, 1.4f)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                val count = (work.imageCount ?: 0) + (work.gifCount ?: 0)
                if (count > 0) {
                    M3GlassChip(
                        text = "$count",
                        icon = Icons.Filled.Image,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = work.workTitle?.ifBlank { "未命名作品" } ?: "未命名作品",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!work.authorNickname.isNullOrBlank()) {
                    Text(
                        text = "@${work.authorNickname}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── 设置底部抽屉 (Settings Modal Bottom Sheet) ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    currentUrl: String,
    urlList: List<String>,
    privacyEnabled: Boolean,
    currentDarkMode: String,
    currentSpaceMode: String,
    proxyEnabled: Boolean,
    proxyType: String,
    proxyHost: String,
    proxyPort: Int,
    onDismiss: () -> Unit,
    onSelectUrl: (String) -> Unit,
    onAddUrl: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onDarkModeSelect: (String) -> Unit,
    onSaveProxyConfig: (enabled: Boolean, type: String, host: String, port: Int) -> Unit
) {
    var newUrlInput by remember { mutableStateOf("") }
    var showAddUrlRow by remember { mutableStateOf(false) }

    var localProxyEnabled by remember(proxyEnabled) { mutableStateOf(proxyEnabled) }
    var localProxyType by remember(proxyType) { mutableStateOf(proxyType) }
    var localProxyHost by remember(proxyHost) { mutableStateOf(proxyHost) }
    var localProxyPort by remember(proxyPort) { mutableStateOf(proxyPort.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "偏好设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentSpaceMode == "gallery") "当前空间：本地·图库" else "当前空间：探索·精选",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── 模块 1：通用外观设置 ──────────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "主题外观",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        listOf(
                            "system" to stringResource(R.string.dark_mode_system),
                            "light" to stringResource(R.string.dark_mode_off),
                            "dark" to stringResource(R.string.dark_mode_on)
                        ).forEach { (mode, label) ->
                            val selected = currentDarkMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onDarkModeSelect(mode) },
                                label = { Text(label) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // ── 模块 2：应用内独立代理设置 ────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "自定义网络代理",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "VPN 自动绕过此 App 时，可手动设置代理端口",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = localProxyEnabled,
                            onCheckedChange = {
                                localProxyEnabled = it
                                val portInt = localProxyPort.toIntOrNull() ?: 7890
                                onSaveProxyConfig(it, localProxyType, localProxyHost.trim(), portInt)
                            }
                        )
                    }

                    if (localProxyEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 协议类型切换
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                "协议：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            listOf("HTTP", "SOCKS").forEach { type ->
                                val selected = localProxyType.equals(type, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        localProxyType = type
                                        val portInt = localProxyPort.toIntOrNull() ?: 7890
                                        onSaveProxyConfig(true, type, localProxyHost.trim(), portInt)
                                    },
                                    label = { Text(type) }
                                )
                            }
                        }

                        // 地址与端口输入
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = localProxyHost,
                                onValueChange = { localProxyHost = it },
                                label = { Text("主机 (如 127.0.0.1)") },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            OutlinedTextField(
                                value = localProxyPort,
                                onValueChange = { localProxyPort = it },
                                label = { Text("端口") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val portInt = localProxyPort.toIntOrNull() ?: 7890
                                onSaveProxyConfig(true, localProxyType, localProxyHost.trim(), portInt)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存并应用代理")
                        }
                    }
                }
            }

            // ── 模块 3：小红书精选专属设置 ───────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = Color(0xFFFF5E62),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "探索内容隐私过滤",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFF5E62).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "XHS专属",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF5E62),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "开启后自动过滤小红书解析内容中的敏感/私密作品",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = privacyEnabled,
                            onCheckedChange = onPrivacyToggle
                        )
                    }
                }
            }

            // ── 模块 4：通用服务端地址配置 ───────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "服务端 API 节点",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "双空间通用网络后端",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    urlList.forEach { url ->
                        val selected = url == currentUrl
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable { onSelectUrl(url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(selected = selected, onClick = { onSelectUrl(url) })
                                    Text(
                                        url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (urlList.size > 1) {
                                    IconButton(onClick = { onRemoveUrl(url) }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.common_delete),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!showAddUrlRow) {
                        TextButton(
                            onClick = { showAddUrlRow = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_add))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            OutlinedTextField(
                                value = newUrlInput,
                                onValueChange = { newUrlInput = it },
                                label = { Text("http://ip:port") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            Button(
                                onClick = {
                                    val trimmed = newUrlInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        onAddUrl(trimmed)
                                        newUrlInput = ""
                                        showAddUrlRow = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
        }
    }
}
