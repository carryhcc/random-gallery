package com.example.randomgallery.android.ui.piclist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.Downloader
import com.example.randomgallery.android.util.MediaKind
import android.content.Context

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

    // 距底部 4 条时触发加载下一页
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 2) viewModel.loadMore()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { XhsTopBar(title = groupName.ifBlank { "套图详情" }, onBack = onBack) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                items.isEmpty() && loading -> XhsLoadingBox(Modifier.fillMaxSize())
                items.isEmpty() && !loading ->
                    XhsEmptyState(error ?: "暂无图片", onRetry = { viewModel.refresh() }, modifier = Modifier.fillMaxSize())
                else -> {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(Spacing.sm),
                        verticalItemSpacing = Spacing.sm,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(items = items, key = { index, pic -> pic.id ?: "idx_$index" }) { _, pic ->
                            val url = ImageUrlResolver.displayUrl(pic.picUrl)
                            var imageLoaded by remember(url) { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = pic.picName,
                                    contentScale = ContentScale.FillWidth,
                                    onSuccess = { imageLoaded = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // 图片加载完成后才显示下载按钮
                                if (imageLoaded) {
                                    IconButton(
                                        onClick = {
                                            Downloader.enqueue(context, url ?: "", MediaKind.IMAGE)
                                            Messenger.show("图片正在下载…")
                                        },
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.40f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.FileDownload,
                                                contentDescription = "下载",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
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