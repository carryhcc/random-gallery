package com.example.randomgallery.android.ui.downloadlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
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
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver

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
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showFilter by remember { mutableStateOf(false) }
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(Unit) {
        if (!viewModel.hasStarted) {
            viewModel.hasStarted = true
            viewModel.init()
            viewModel.refresh()
        }
    }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            XhsTopBar(
                title = "下载列表",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showFilter = !showFilter }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "筛选")
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
                            error ?: "暂无内容",
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        )
                    else -> {
                        LazyVerticalStaggeredGrid(
                            state = gridState,
                            columns = StaggeredGridCells.Fixed(2),
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

@Composable
private fun WorkCard(work: XhsWorkListVO, onClick: () -> Unit) {
    val ratio = 3f / 4f   // 固定竖版比例，避免 Crop 裁切随机性

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            AsyncImage(
                model = ImageUrlResolver.displayUrl(work.coverImageUrl),
                contentDescription = work.workTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(Modifier.padding(Spacing.md)) {
                Text(
                    text = work.workTitle ?: "无标题",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = work.authorNickname ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val imgCount = work.imageCount ?: 0
                    val vidCount = work.gifCount ?: 0
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (imgCount > 0) {
                            Icon(Icons.Filled.Image, null, tint = MaterialTheme.xhs.textTertiary, modifier = Modifier.size(11.dp))
                            Text("$imgCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.xhs.textTertiary)
                        }
                        if (vidCount > 0) {
                            Icon(Icons.Filled.VideoLibrary, null, tint = MaterialTheme.xhs.accentBlue, modifier = Modifier.size(11.dp))
                            Text("$vidCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.xhs.textTertiary)
                        }
                    }
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
            label = { Text("关键词") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )

        if (authors.isNotEmpty()) {
            Text("作者", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                authors.take(8).forEach { (name, id) ->
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
            Text("标签", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                tags.take(8).forEach { (name, id) ->
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("重置") }
            Button(
                onClick = { onApply(authorId, tagId, kw) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("应用") }
        }
    }
}
