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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.Downloader
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

    // 网格清晰图目标尺寸：单格宽度 * 2（有界，避免按原图下载）
    val gridFullSizePx = with(LocalDensity.current) {
        val cellDp = (LocalConfiguration.current.screenWidthDp - Spacing.sm.value * 3) / 2
        (cellDp * density * 2).toInt()
    }

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
        topBar = { XhsTopBar(title = groupName.ifBlank { stringResource(R.string.group_detail_fallback) }, onBack = onBack) }
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
                    XhsEmptyState(error ?: stringResource(R.string.piclist_empty), onRetry = { viewModel.refresh() }, modifier = Modifier.fillMaxSize())
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
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                SmartImage(
                                    url = url,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    fullSize = gridFullSizePx,
                                    onFullLoaded = { imageLoaded = true },
                                    contentDescription = pic.picName
                                )
                                // 图片加载完成后才显示下载按钮
                                if (imageLoaded) {
                                    IconButton(
                                        onClick = {
                                            if (!url.isNullOrBlank()) {
                                                Downloader.enqueue(context, url, MediaKind.IMAGE)
                                                Messenger.show(context.getString(R.string.piclist_downloading))
                                            }
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
                                                contentDescription = stringResource(R.string.common_download),
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