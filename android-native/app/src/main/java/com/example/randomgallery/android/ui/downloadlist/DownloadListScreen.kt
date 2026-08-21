package com.example.randomgallery.android.ui.downloadlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
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
    val works by viewModel.works.collectAsStateWithLifecycle()
    val authors by viewModel.authors.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showFilter by remember { mutableStateOf(false) }
    val gridState = rememberLazyStaggeredGridState()

    // Infinite scroll
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4) viewModel.loadMore()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            XhsTopBar(
                title = stringResource(R.string.dl_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showFilter = !showFilter }) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.dl_filter))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showFilter) {
                FilterPanel(
                    authors = authors.map { Pair(it.authorNickname ?: it.authorId ?: "", it.authorId ?: "") },
                    tags = tags.map { Pair(it.tagName ?: "", it.id ?: 0L) },
                    selectedAuthorId = viewModel.authorId,
                    selectedTagId = viewModel.tagId,
                    keyword = viewModel.keyword ?: "",
                    onApply = { authorId, tagId, kw ->
                        viewModel.authorId = authorId
                        viewModel.tagId = tagId
                        viewModel.keyword = kw.ifBlank { null }
                        viewModel.refresh()
                        showFilter = false
                    },
                    onReset = {
                        viewModel.resetFilters()
                        showFilter = false
                    }
                )
                XhsDivider()
            }

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
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { viewModel.refreshAll() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyVerticalStaggeredGrid(
                            state = gridState,
                            columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalItemSpacing = Spacing.md,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(items = works, key = { index, work -> work.workId ?: work.id ?: "idx_$index" }) { _, work ->
                                WorkCard(work = work, onClick = {
                                    onWorkClick(
                                        work.workId ?: "",
                                        ImageUrlResolver.displayUrl(work.coverImageUrl)
                                    )
                                })
                            }
                            if (loading) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    XhsLoadingBox(Modifier.fillMaxWidth().height(48.dp))
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkCard(work: XhsWorkListVO, onClick: () -> Unit) {
    val ratio = 3f / 4f   // 默认竖版比例
    val imgCount = work.imageCount ?: 0
    val vidCount = work.gifCount ?: 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bouncyClickable(onClick = onClick)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageUrlResolver.displayUrl(work.coverImageUrl),
                    contentDescription = work.workTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // 潮流悬浮胶囊图层：根据媒体类型展示
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (vidCount > 0) {
                        XhsFloatingPill(text = "$vidCount", icon = Icons.Filled.VideoLibrary)
                    }
                    if (imgCount > 0) {
                        XhsFloatingPill(text = "$imgCount", icon = Icons.Filled.Image)
                    }
                }
            }

            Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                Text(
                    text = work.workTitle ?: stringResource(R.string.dl_untitled),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!work.authorNickname.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "@${work.authorNickname}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    authors: List<Pair<String, String>>,
    tags: List<Pair<String, Long>>,
    selectedAuthorId: String?,
    selectedTagId: Long?,
    keyword: String,
    onApply: (String?, Long?, String) -> Unit,
    onReset: () -> Unit
) {
    var kw by remember { mutableStateOf(keyword) }
    var authorId by remember { mutableStateOf(selectedAuthorId) }
    var tagId by remember { mutableStateOf(selectedTagId) }
    var showMoreFilters by remember { mutableStateOf(selectedAuthorId != null || selectedTagId != null) }

    // 随机推荐：每次展开面板（重新进入组合）打乱后取 8 个，而非固定前 8 个
    val recommendedAuthors = remember(authors) { authors.shuffled().take(8) }
    val recommendedTags = remember(tags) { tags.shuffled().take(8) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        OutlinedTextField(
            value = kw,
            onValueChange = { kw = it },
            label = { Text(stringResource(R.string.dl_keyword)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )

        // 更多筛选（作者 / 标签）展开与收起
        if (authors.isNotEmpty() || tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showMoreFilters = !showMoreFilters }
                    .padding(vertical = Spacing.xs, horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(if (showMoreFilters) R.string.dl_hide_more_filters else R.string.dl_more_filters),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (showMoreFilters) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showMoreFilters) {
            if (authors.isNotEmpty()) {
                Text(stringResource(R.string.dl_author), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    recommendedAuthors.forEach { (name, id) ->
                        FilterChip(
                            selected = authorId == id,
                            onClick = { authorId = if (authorId == id) null else id },
                            label = { Text(name, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            if (tags.isNotEmpty()) {
                Text(stringResource(R.string.dl_tag), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    recommendedTags.forEach { (name, id) ->
                        FilterChip(
                            selected = tagId == id,
                            onClick = { tagId = if (tagId == id) null else id },
                            label = { Text(name, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(
                onClick = {
                    authorId = null
                    tagId = null
                    kw = ""
                    onReset()
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.dl_reset)) }
            Button(
                onClick = { onApply(authorId, tagId, kw) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(stringResource(R.string.dl_apply)) }
        }
    }
}
