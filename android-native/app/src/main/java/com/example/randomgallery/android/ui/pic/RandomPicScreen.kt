package com.example.randomgallery.android.ui.pic

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.downloadToPublic
import com.example.randomgallery.android.util.toast

@Composable
fun RandomPicScreen(
    viewModel: RandomPicViewModel,
    onBack: () -> Unit,
    onGroupClick: (groupId: Long, groupName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val picState by viewModel.picState.observeAsState()
    val groupState by viewModel.groupState.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var imageUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadRandomPic() }

    LaunchedEffect(picState) {
        picState?.onSuccess { pic ->
            imageUrl = ImageUrlResolver.displayUrl(pic.picUrl) ?: ""
        }?.onFailure {
            snackbarHostState.showSnackbar(it.message ?: "加载失败")
        }
    }

    TopSnackbarBox(snackbarHostState) {
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            XhsTopBar(
                title = groupState?.getOrNull()?.groupName ?: "随机一图",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadRandomPic() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "换一张", tint = NeutralWhite)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { viewModel.loadRandomPic() })
                }
        ) {
            when {
                picState == null -> XhsLoadingBox(Modifier.fillMaxSize())
                picState?.isFailure == true && imageUrl.isBlank() ->
                    XhsEmptyState(
                        picState!!.exceptionOrNull()?.message ?: "加载失败",
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

            // Bottom gradient + download button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    picState?.getOrNull()?.groupId?.let { gid ->
                        val groupName = groupState?.getOrNull()?.groupName ?: "套图详情"
                        FilledTonalButton(
                            onClick = { onGroupClick(gid, groupName) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = NeutralWhite
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("查看套图", fontWeight = FontWeight.Medium) }
                    }
                    FloatingActionButton(
                        onClick = {
                            if (imageUrl.isNotBlank()) {
                                context.downloadToPublic(imageUrl, "random_${System.currentTimeMillis()}.jpg")
                                context.toast("已加入下载队列")
                            }
                        },
                        containerColor = XhsRed,
                        contentColor = NeutralWhite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "下载", modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Hint label
            if (picState?.isSuccess == true) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Spacing.lg)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text("双击换一张", style = MaterialTheme.typography.labelSmall, color = NeutralWhite.copy(alpha = 0.7f))
                }
            }
        }
    }
    }
}
