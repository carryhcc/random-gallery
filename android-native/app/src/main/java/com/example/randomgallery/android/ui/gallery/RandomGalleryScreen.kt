package com.example.randomgallery.android.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme
import com.example.randomgallery.android.ui.theme.Spacing
import com.example.randomgallery.android.util.ImageUrlResolver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.randomgallery.android.R

/** 随机图库瀑布流（小红书风）。复用现有 RandomGalleryViewModel，只重做 UI。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomGalleryScreen(
    viewModel: RandomGalleryViewModel,
    onGroupClick: (GroupVO) -> Unit,
    onBack: () -> Unit
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val gridState = rememberLazyStaggeredGridState()
    // 图片真实宽高比缓存（URL → ratio），避免重组/复用时跳动
    val ratioCache = remember { mutableStateMapOf<String, Float>() }

    // 首次进入加载一次；从返回栈回来时复用已有数据，不重复拉取
    LaunchedEffect(Unit) {
        if (viewModel.groups.value.isEmpty()) viewModel.refresh()
    }

    // 触底自动加载更多
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastIndex to info.totalItemsCount
        }.collect { (lastIndex, total) ->
            if (total > 0 && lastIndex >= total - 4) viewModel.loadMore()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                groups.isEmpty() && loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                groups.isEmpty() && !loading -> {
                    EmptyState(
                        message = error ?: stringResource(R.string.common_empty),
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalItemSpacing = Spacing.md,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = groups) { group ->
                            FeedCard(group = group, ratioCache = ratioCache, onClick = { onGroupClick(group) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCard(
    group: GroupVO,
    ratioCache: MutableMap<String, Float>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val url = ImageUrlResolver.displayUrl(group.groupUrl) ?: ""
    // 未知尺寸时先用 1:1 占位，加载成功后过渡到真实比例（限制在 3:4 ~ 4:3 之间）
    val ratio = if (url.isBlank()) 1f else ratioCache[url] ?: 1f

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
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = group.groupName,
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (url.isNotBlank() && size.width > 0f && size.height > 0f) {
                        ratioCache[url] = (size.width / size.height).coerceIn(0.75f, 1.33f)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.groupName ?: stringResource(R.string.group_unnamed),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${group.groupCount ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedCardPreview() {
    RandomGalleryTheme {
        FeedCard(
            group = GroupVO(groupId = 3L, groupName = "夏日海边写真合集 清新自然", groupUrl = null, groupCount = 24),
            ratioCache = remember { mutableStateMapOf() },
            onClick = {}
        )
    }
}
