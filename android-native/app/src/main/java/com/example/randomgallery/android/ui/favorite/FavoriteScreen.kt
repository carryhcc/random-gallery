package com.example.randomgallery.android.ui.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.RandomGifVO
import com.example.randomgallery.android.ui.common.XhsEmptyState
import com.example.randomgallery.android.ui.common.XhsLoadingBox
import com.example.randomgallery.android.ui.common.XhsTopBar
import com.example.randomgallery.android.ui.common.bouncyClickable
import com.example.randomgallery.android.ui.theme.Spacing
import com.example.randomgallery.android.util.ImageUrlResolver

/**
 * 我的收藏（REQ-01）。
 * 列表按收藏时间倒序、分页加载，支持取消收藏即时移除。
 */
@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel,
    onWorkClick: (workId: String) -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 触底加载下一页（提前 3 条触发）
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, items.size) {
        if (nearEnd) viewModel.loadMore()
    }

    val currentError = error

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { XhsTopBar(title = stringResource(R.string.favorite_title), onBack = onBack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                items.isEmpty() && loading -> XhsLoadingBox(Modifier.fillMaxSize())

                items.isEmpty() && currentError != null -> XhsEmptyState(
                    message = currentError,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )

                items.isEmpty() -> XhsEmptyState(
                    message = stringResource(R.string.favorite_empty),
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(items, key = { it.id ?: 0L }) { gif ->
                        FavoriteItem(
                            gif = gif,
                            onOpen = onWorkClick,
                            onRemove = { id -> viewModel.removeFavorite(id) }
                        )
                    }
                    if (loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    gif: RandomGifVO,
    onOpen: (workId: String) -> Unit,
    onRemove: (Long) -> Unit
) {
    val context = LocalContext.current
    val author = gif.authorNickname

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable { gif.workId?.let(onOpen) }
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 动图实际是视频文件，由 Coil 的 VideoFrameDecoder 抽首帧作为封面
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val url = ImageUrlResolver.rawUrl(gif.mediaUrl)
            if (url.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gif.workTitle?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.favorite_untitled),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!author.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { gif.id?.let(onRemove) }) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.favorite_remove),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
