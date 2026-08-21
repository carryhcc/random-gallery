package com.example.randomgallery.android.ui.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import com.example.randomgallery.android.util.ImageUrlResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupListViewModel,
    onGroupClick: (GroupVO) -> Unit,
    onBack: () -> Unit
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val pageInfo by viewModel.pageInfo.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var keyword by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()
    val atTop by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 } }

    LaunchedEffect(Unit) {
        if (groups.isEmpty()) viewModel.query(null)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { XhsTopBar(title = stringResource(R.string.group_title), onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── 搜索栏（顶部时显示，下滑时隐藏）─────────────────────
            AnimatedVisibility(
                visible = atTop,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.common_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                BasicTextField(
                                    value = keyword,
                                    onValueChange = {
                                        keyword = it
                                        if (it.isBlank()) viewModel.query(null)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { viewModel.query(keyword.trim().ifBlank { null }) }),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (keyword.isBlank()) {
                                                Text(
                                                    text = stringResource(R.string.group_search_hint),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                if (keyword.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            keyword = ""
                                            viewModel.query(null)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    XhsDivider()
                }
            }

            // ── 内容区 ────────────────────────────────────────────────
            Box(Modifier.weight(1f)) {
                when {
                    groups.isEmpty() && error != null ->
                        XhsEmptyState(error!!, onRetry = { viewModel.query(null) }, modifier = Modifier.fillMaxSize())
                    groups.isEmpty() && loading ->
                        XhsLoadingBox(Modifier.fillMaxSize())
                    else ->
                        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                            isRefreshing = loading && groups.isNotEmpty(),
                            onRefresh = { viewModel.query(keyword.trim().ifBlank { null }, pageIndex = 1) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(items = groups, key = { index, group -> group.groupId ?: "idx_$index" }) { _, group ->
                                    GroupCard(group = group, onClick = { onGroupClick(group) })
                                }
                                item(span = { GridItemSpan(2) }) {
                                    Spacer(Modifier.height(Spacing.md))
                                }
                            }
                        }
                }
            }

            // ── 分页栏 ────────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.prevPage() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, stringResource(R.string.group_prev_page), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(pageInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = { viewModel.nextPage() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Filled.ChevronRight, stringResource(R.string.group_next_page), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupVO, onClick: () -> Unit) {
    val coverUrl = ImageUrlResolver.displayUrl(group.groupUrl)
    val count = group.groupCount ?: 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bouncyClickable(onClick = onClick)
    ) {
        Box {
            // 封面图
            AsyncImage(
                model = coverUrl,
                contentDescription = group.groupName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // 潮流悬浮胶囊：图片张数置于右上角
            if (count > 0) {
                XhsFloatingPill(
                    text = "$count",
                    icon = Icons.Filled.Image,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm)
                )
            }

            // 底部渐变蒙层 + 文字
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                        )
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.md)
            ) {
                Text(
                    text = group.groupName ?: stringResource(R.string.group_unnamed),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
