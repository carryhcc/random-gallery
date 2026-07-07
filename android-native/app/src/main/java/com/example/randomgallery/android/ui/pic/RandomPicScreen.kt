package com.example.randomgallery.android.ui.pic

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.Downloader
import com.example.randomgallery.android.util.MediaKind

@Composable
fun RandomPicScreen(
    viewModel: RandomPicViewModel,
    onBack: () -> Unit,
    onGroupClick: (groupId: Long, groupName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val picState by viewModel.picState.collectAsStateWithLifecycle()
    val groupState by viewModel.groupState.collectAsStateWithLifecycle()

    var imageUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadRandomPic() }

    LaunchedEffect(picState) {
        when (val state = picState) {
            is UiState.Success -> imageUrl = ImageUrlResolver.displayUrl(state.data.picUrl) ?: ""
            is UiState.Error -> Messenger.show(state.message, isError = true)
            else -> Unit
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { viewModel.loadRandomPic() })
            }
    ) {
            when {
                picState is UiState.Loading -> XhsLoadingBox(Modifier.fillMaxSize())
                picState is UiState.Error && imageUrl.isBlank() ->
                    XhsEmptyState(
                        (picState as UiState.Error).message,
                        onRetry = { viewModel.loadRandomPic() },
                        modifier = Modifier.fillMaxSize()
                    )
                else -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 顶部沉浸式操作条：黑色渐变上覆盖白色图标/标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Color.White)
                }
                Text(
                    text = groupState?.groupName ?: stringResource(R.string.pic_title_fallback),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.loadRandomPic() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.pic_change), tint = Color.White)
                }
            }

            // 底部渐变 + 操作按钮（避开手势导航条）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    (picState as? UiState.Success)?.data?.groupId?.let { gid ->
                        val groupName = groupState?.groupName ?: stringResource(R.string.group_detail_fallback)
                        FilledTonalButton(
                            onClick = { onGroupClick(gid, groupName) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text(stringResource(R.string.pic_view_group), fontWeight = FontWeight.Medium) }
                    }
                    FloatingActionButton(
                        onClick = {
                            if (imageUrl.isNotBlank()) {
                                Downloader.enqueue(context, imageUrl, MediaKind.IMAGE)
                                Messenger.show(context.getString(R.string.pic_download_queued))
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.common_download), modifier = Modifier.size(22.dp))
                    }
                }
            }

            // 提示标签（顶部操作条下方）
            if (picState is UiState.Success) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text(stringResource(R.string.pic_double_tap_hint), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
}
