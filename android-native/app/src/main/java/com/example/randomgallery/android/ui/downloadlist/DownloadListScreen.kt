package com.example.randomgallery.android.ui.downloadlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.size.Scale
import coil.request.ImageRequest
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    viewModel: DownloadListViewModel,
    onWorkClick: (workId: String, coverImageUrl: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val works by viewModel.works.collectAsStateWithLifecycle()
    val authors by viewModel.authors.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf(viewModel.keyword ?: "") }

    val gridState = rememberLazyStaggeredGridState()
    val ratioCache = remember { mutableStateMapOf<String, Float>() }

    // 触底自动加载更多
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4) viewModel.loadMore()
        }
    }

    val hasActiveFilter = viewModel.authorId != null || viewModel.tagId != null || !viewModel.keyword.isNullOrBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dl_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // 筛选入口带红点/高亮徽标
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasActiveFilter) XhsRed.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (hasActiveFilter) {
                                        Badge(containerColor = XhsRed)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = stringResource(R.string.dl_filter),
                                    tint = if (hasActiveFilter) XhsRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── 1. 顶部紧凑流式搜索栏 (轻量高度 40dp，完全不挡瀑布流) ───────────
            CompactSearchHeaderBar(
                keyword = searchKeyword,
                onKeywordChange = {
                    searchKeyword = it
                    if (it.isBlank() && viewModel.keyword != null) {
                        viewModel.keyword = null
                        viewModel.refresh()
                    }
                },
                onSearch = {
                    viewModel.keyword = searchKeyword.trim().ifBlank { null }
                    viewModel.refresh()
                },
                selectedAuthorName = authors.firstOrNull { it.authorId == viewModel.authorId }?.authorNickname,
                selectedTagName = tags.firstOrNull { it.id == viewModel.tagId }?.tagName,
                onClearAuthor = {
                    viewModel.authorId = null
                    viewModel.refresh()
                },
                onClearTag = {
                    viewModel.tagId = null
                    viewModel.refresh()
                },
                onOpenFilterSheet = { showFilterSheet = true }
            )

            // ── 2. 作品双列瀑布流内容区 ──────────────────────────────
            Box(Modifier.weight(1f)) {
                when {
                    works.isEmpty() && loading -> XhsLoadingBox(Modifier.fillMaxSize())
                    works.isEmpty() && !loading ->
                        XhsEmptyState(
                            error ?: stringResource(R.string.common_empty),
                            onRetry = { viewModel.refreshAll() },
                            modifier = Modifier.fillMaxSize()
                        )
                    else -> {
                        val isScrolling = gridState.isScrollInProgress
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { viewModel.refreshAll() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyVerticalStaggeredGrid(
                                state = gridState,
                                columns = StaggeredGridCells.Adaptive(minSize = 165.dp),
                                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = 80.dp),
                                verticalItemSpacing = Spacing.md,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = works,
                                    key = { index, item -> item.workId ?: item.id ?: index.toLong() }
                                ) { index, work ->
                                    StaggeredItemEntrance(index = index) {
                                        DownloadWorkGridCard(
                                            work = work,
                                            ratioCache = ratioCache,
                                            isScrolling = isScrolling,
                                            onClick = {
                                                work.workId?.let { wid ->
                                                    onWorkClick(wid, work.coverImageUrl ?: "")
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
        }
    }

    // ── 3. 专属过滤底部抽屉 (ModalBottomSheet，彻底告别原先挤占半屏的死板展开) ──
    if (showFilterSheet) {
        DownloadFilterBottomSheet(
            authors = authors.map { Pair(it.authorNickname ?: it.authorId ?: "", it.authorId ?: "") },
            tags = tags.map { Pair(it.tagName ?: "", it.id ?: 0L) },
            selectedAuthorId = viewModel.authorId,
            selectedTagId = viewModel.tagId,
            keyword = viewModel.keyword ?: "",
            onDismiss = { showFilterSheet = false },
            onApply = { authorId, tagId, kw ->
                viewModel.authorId = authorId
                viewModel.tagId = tagId
                viewModel.keyword = kw.ifBlank { null }
                searchKeyword = kw
                viewModel.refresh()
                showFilterSheet = false
            },
            onReset = {
                viewModel.resetFilters()
                searchKeyword = ""
                showFilterSheet = false
            }
        )
    }
}

// ── 顶部紧凑搜索与激活筛选胶囊栏 ─────────────────────────────────────

@Composable
private fun CompactSearchHeaderBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedAuthorName: String?,
    selectedTagName: String?,
    onClearAuthor: () -> Unit,
    onClearTag: () -> Unit,
    onOpenFilterSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 搜索框：轻量胶囊化
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                androidx.compose.foundation.text.BasicTextField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (keyword.isBlank()) {
                            Text(
                                text = "搜索下载作品关键词...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                )

                if (keyword.isNotBlank()) {
                    IconButton(
                        onClick = { onKeywordChange("") },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "清空",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // 如果存在激活的作者或标签过滤，以横向轻量胶囊展示
        if (selectedAuthorName != null || selectedTagName != null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedAuthorName != null) {
                    item {
                        ActiveFilterTag(
                            label = "@$selectedAuthorName",
                            icon = Icons.Filled.Person,
                            onClear = onClearAuthor
                        )
                    }
                }
                if (selectedTagName != null) {
                    item {
                        ActiveFilterTag(
                            label = "#$selectedTagName",
                            icon = Icons.Filled.Tag,
                            onClear = onClearTag
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterTag(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClear: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClear, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "移除", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
            }
        }
    }
}

// ── 作品瀑布流卡片 ───────────────────────────────────────────────────

@Composable
private fun DownloadWorkGridCard(
    work: XhsWorkListVO,
    ratioCache: SnapshotStateMap<String, Float>,
    isScrolling: Boolean = false,
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
                        // 三级缓存策略：强制读写磁盘与内存缓存
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(if (isScrolling) CachePolicy.READ_ONLY else CachePolicy.ENABLED)
                        .scale(Scale.FIT)
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

                // 媒体数量微标
                val totalCount = (work.imageCount ?: 0) + (work.gifCount ?: 0)
                if (totalCount > 0) {
                    M3GlassChip(
                        text = "$totalCount",
                        icon = if ((work.gifCount ?: 0) > 0) Icons.Filled.Animation else Icons.Filled.Image,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = work.workTitle?.ifBlank { stringResource(R.string.dl_untitled) } ?: stringResource(R.string.dl_untitled),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── 专属筛选底部抽屉 (ModalBottomSheet 优雅筛选) ─────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DownloadFilterBottomSheet(
    authors: List<Pair<String, String>>,
    tags: List<Pair<String, Long>>,
    selectedAuthorId: String?,
    selectedTagId: Long?,
    keyword: String,
    onDismiss: () -> Unit,
    onApply: (String?, Long?, String) -> Unit,
    onReset: () -> Unit
) {
    var kw by remember { mutableStateOf(keyword) }
    var authorId by remember { mutableStateOf(selectedAuthorId) }
    var tagId by remember { mutableStateOf(selectedTagId) }

    val recommendedAuthors = remember(authors) { authors.shuffled().take(10) }
    val recommendedTags = remember(tags) { tags.shuffled().take(12) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                .padding(bottom = Spacing.lg)
                .fillMaxHeight(0.75f),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 顶栏：标题与重置按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("筛选已下载作品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = {
                    kw = ""
                    authorId = null
                    tagId = null
                    onReset()
                }) {
                    Text("重置全部", color = MaterialTheme.colorScheme.error)
                }
            }

            // 滚动内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // 关键词
                OutlinedTextField(
                    value = kw,
                    onValueChange = { kw = it },
                    label = { Text(stringResource(R.string.dl_keyword)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 作者流式标签
                if (recommendedAuthors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.dl_author), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            recommendedAuthors.forEach { (name, id) ->
                                val selected = authorId == id
                                FilterChip(
                                    selected = selected,
                                    onClick = { authorId = if (authorId == id) null else id },
                                    label = { Text(name, maxLines = 1) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(14.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // 标签 FlowRow
                if (recommendedTags.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.dl_tag), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            recommendedTags.forEach { (name, id) ->
                                val selected = tagId == id
                                FilterChip(
                                    selected = selected,
                                    onClick = { tagId = if (tagId == id) null else id },
                                    label = { Text(name, maxLines = 1) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(14.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 底部应用大按钮
            Button(
                onClick = { onApply(authorId, tagId, kw) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("应用筛选条件", fontWeight = FontWeight.Bold)
            }
        }
    }
}
