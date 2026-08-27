package com.example.randomgallery.android.ui.download

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.DownloadTaskStatsVO
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManageScreen(
    viewModel: DownloadManageViewModel,
    onBack: () -> Unit,
    onViewDetail: (workId: String) -> Unit
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val lastResolvedUrl by viewModel.lastResolvedUrl.collectAsStateWithLifecycle()
    val autoReadClipboard by viewModel.autoReadClipboard.collectAsStateWithLifecycle()

    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val historyPage by viewModel.historyPage.collectAsStateWithLifecycle()
    val historyTotalPages by viewModel.historyTotalPages.collectAsStateWithLifecycle()
    val historyLoading by viewModel.historyLoading.collectAsStateWithLifecycle()
    val historyError by viewModel.historyError.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var taskToDelete by remember { mutableStateOf<XhsDownloadTaskVO?>(null) }

    // Android 12+ 获焦读剪贴板
    val view = LocalView.current
    DisposableEffect(view, autoReadClipboard) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && autoReadClipboard) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                val extracted = DownloadManageViewModel.extractHttpUrl(text)
                if (!extracted.isNullOrBlank()) urlInput = extracted
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }

    LaunchedEffect(Unit) {
        viewModel.submitEvents.collect { result ->
            val msg = if (result.isSuccess) {
                val urlHint = lastResolvedUrl?.let { url ->
                    val short = if (url.length > 40) "…${url.takeLast(30)}" else url
                    "\n$short"
                } ?: ""
                "${result.getOrDefault("提交成功")}$urlHint"
            } else {
                result.exceptionOrNull()?.message ?: "提交失败"
            }
            Messenger.show(msg, isError = result.isFailure)
            if (result.isSuccess) urlInput = ""
        }
    }

    LaunchedEffect(Unit) {
        viewModel.historyEvents.collect { result ->
            val msg = result.exceptionOrNull()?.message ?: result.getOrDefault("操作成功")
            Messenger.show(msg, isError = result.isFailure)
        }
    }

    // 页面活跃时每 3s 轮询进度
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.pollHistory()
                delay(3000)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "下载与解析管理",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = XhsRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "XHS HUB",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = XhsRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshHistory() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.common_refresh))
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
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ── 1. 顶部 Hero 智能解析舱 ─────────────────────────────
            DownloadHeroInputCard(
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                loading = loading,
                autoReadClipboard = autoReadClipboard,
                onAutoClipboardToggle = { viewModel.setAutoReadClipboard(it) },
                onPasteClipboard = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                    val extracted = DownloadManageViewModel.extractHttpUrl(text)
                    if (!extracted.isNullOrBlank()) urlInput = extracted
                    else if (!text.isNullOrBlank()) urlInput = text
                },
                onSubmit = {
                    if (urlInput.isNotBlank() && !loading) viewModel.submit(urlInput)
                }
            )

            // ── 2. 任务历史看板与状态过滤胶囊 ────────────────────────
            M3GlassCard(
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(Spacing.sm)) {
                    // 状态筛选 Tab 胶囊栏
                    TaskStatsFilterBar(
                        stats = stats,
                        currentFilter = statusFilter,
                        onFilterSelect = { viewModel.setStatusFilter(it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    when {
                        history.isEmpty() && historyLoading -> {
                            XhsLoadingBox(Modifier.weight(1f).fillMaxWidth())
                        }
                        history.isEmpty() && !historyLoading -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = if (statusFilter != null) "无该状态下的任务记录" else stringResource(R.string.dm_history_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        else -> {
                            val historyItems = history.filter { it.id != null }
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                items(historyItems, key = { it.id!! }) { task ->
                                    ModernTaskHistoryCard(
                                        task = task,
                                        onRetry = { viewModel.retryTask(task.id!!) },
                                        onDelete = { taskToDelete = task },
                                        onViewDetail = { workId -> onViewDetail(workId) }
                                    )
                                }
                            }
                        }
                    }

                    // 分页控制器
                    if (historyTotalPages > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.xs),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.prevHistoryPage() },
                                enabled = historyPage > 1,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.dm_prev_page), style = MaterialTheme.typography.labelMedium)
                            }

                            Text(
                                stringResource(R.string.dm_page_fmt, historyPage, historyTotalPages),
                                style = MaterialTheme.typography.labelMedium.tabularNumbers,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = Spacing.md)
                            )

                            OutlinedButton(
                                onClick = { viewModel.nextHistoryPage() },
                                enabled = historyPage < historyTotalPages,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.dm_next_page), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认 Dialog
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("删除下载任务记录", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "确定要删除该条记录吗？${taskToDelete?.workTitle?.let { "\n《$it》" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        taskToDelete?.id?.let { viewModel.deleteTask(it) }
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ── 任务状态交互筛选栏 (可点击切换查询条件) ──────────────────────────

@Composable
private fun TaskStatsFilterBar(
    stats: DownloadTaskStatsVO,
    currentFilter: Int?,
    onFilterSelect: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 全部
        StatsFilterChip(
            label = "全部",
            count = stats.total,
            selected = currentFilter == null,
            activeColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelect(null) }
        )

        // 进行中 / 排队中 (status = 0)
        StatsFilterChip(
            label = "进行中",
            count = stats.waitingCount,
            selected = currentFilter == 0,
            activeColor = Color(0xFF4A90D9),
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelect(if (currentFilter == 0) null else 0) }
        )

        // 已完成 (status = 1)
        StatsFilterChip(
            label = "已完成",
            count = stats.completedCount,
            selected = currentFilter == 1,
            activeColor = Color(0xFF3BAD7A),
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelect(if (currentFilter == 1) null else 1) }
        )

        // 失败 (status = 2)
        StatsFilterChip(
            label = "失败",
            count = stats.failedCount,
            selected = currentFilter == 2,
            activeColor = Color(0xFFE05252),
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelect(if (currentFilter == 2) null else 2) }
        )
    }
}

@Composable
private fun StatsFilterChip(
    label: String,
    count: Long,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .height(34.dp)
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(3.dp))
            Surface(
                shape = CircleShape,
                color = if (selected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.tabularNumbers,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

// ── 顶部 Hero 智能输入舱 ───────────────────────────────────────────────

@Composable
private fun DownloadHeroInputCard(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    loading: Boolean,
    autoReadClipboard: Boolean,
    onAutoClipboardToggle: (Boolean) -> Unit,
    onPasteClipboard: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // 头部标题与自动监听 Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(XhsRed, Color(0xFFFF5E62))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "智能链接解析",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "支持小红书图文、实况 Live 及视频笔记",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (autoReadClipboard) XhsRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.bouncyClickable { onAutoClipboardToggle(!autoReadClipboard) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (autoReadClipboard) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (autoReadClipboard) XhsRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "自动读剪贴板",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (autoReadClipboard) XhsRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (autoReadClipboard) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 输入框
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(
                        "粘贴小红书分享链接 (如 http://xhslink.com/...) 或完整文本",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XhsRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                trailingIcon = {
                    IconButton(
                        onClick = onPasteClipboard,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Filled.ContentPaste,
                            contentDescription = stringResource(R.string.dm_paste),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )

            // 提交大按钮（小红书珊瑚红渐变质感）
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = if (loading || urlInput.isBlank()) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .bouncyClickable(
                        enabled = !loading && urlInput.isNotBlank(),
                        onClick = onSubmit
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (urlInput.isNotBlank() && !loading) {
                                Brush.linearGradient(listOf(XhsRed, Color(0xFFFF5E62)))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "正在提交与解析...",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = if (urlInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "即刻解析并加入下载",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (urlInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 现代化任务记录卡片 ───────────────────────────────────────────────

@Composable
private fun ModernTaskHistoryCard(
    task: XhsDownloadTaskVO,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    val hasTitle = !task.workTitle.isNullOrBlank()
    val isSuccess = task.status == 1 || task.status == 3
    val isFailed = task.status == 2
    val isWaiting = task.status == 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        border = BorderStroke(
            1.dp,
            if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 状态徽标与时间行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态胶囊
                TaskStatusPill(task)

                Text(
                    text = task.createTime ?: "",
                    style = MaterialTheme.typography.labelSmall.tabularNumbers,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // 标题 / 链接
            Text(
                text = if (hasTitle) task.workTitle!! else task.url ?: "无链接",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasTitle) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 错误详情（如果失败）
            if (isFailed && !task.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = task.errorMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 底部操作区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 失败状态操作：重新解析 + 删除
                if (isFailed) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("删除", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重新解析", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // 进行中状态支持删除
                if (isWaiting) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("取消/删除", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // 成功状态操作：查看作品 + 删除
                if (isSuccess && !task.workId.isNullOrBlank()) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "删除记录",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Button(
                        onClick = { onViewDetail(task.workId!!) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("查看作品", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── 彩色状态胶囊 ─────────────────────────────────────────────────────

@Composable
private fun TaskStatusPill(task: XhsDownloadTaskVO) {
    val (text, icon, bgTint, textTint) = when (task.status) {
        0 -> Quadruple(
            stringResource(R.string.dm_status_waiting),
            Icons.Filled.HourglassTop,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
        1 -> Quadruple(
            stringResource(R.string.dm_status_completed),
            Icons.Filled.CheckCircle,
            Color(0xFFE8F7F1),
            Color(0xFF3BAD7A)
        )
        3 -> Quadruple(
            stringResource(R.string.dm_status_updated),
            Icons.Filled.Update,
            Color(0xFFE8F1FB),
            Color(0xFF4A90D9)
        )
        else -> Quadruple(
            stringResource(R.string.dm_status_failed),
            Icons.Filled.Cancel,
            Color(0xFFFDECEC),
            Color(0xFFE05252)
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgTint
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textTint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textTint
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
