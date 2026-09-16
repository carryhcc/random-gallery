package com.example.randomgallery.android.ui.download

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
    val taskActionBusy by viewModel.taskActionBusy.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var isInputExpanded by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<XhsDownloadTaskVO?>(null) }

    // Android 12+ 获焦读剪贴板
    val view = LocalView.current
    DisposableEffect(view, autoReadClipboard) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && autoReadClipboard) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) {
                    val extracted = DownloadManageViewModel.extractHttpUrl(text)
                    if (!extracted.isNullOrBlank()) {
                        // 保留用户复制的完整原始文本，不提前截断
                        urlInput = text
                        isInputExpanded = true
                    }
                }
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
            if (result.isSuccess) {
                urlInput = ""
                isInputExpanded = false
            }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // ── 1. 顶部智能解析舱（默认紧凑，点击展开） ──
            CollapsibleDownloadInputCard(
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                isExpanded = isInputExpanded || urlInput.isNotBlank(),
                onToggleExpand = { isInputExpanded = !isInputExpanded },
                loading = loading,
                autoReadClipboard = autoReadClipboard,
                onAutoClipboardToggle = { viewModel.setAutoReadClipboard(it) },
                onPasteClipboard = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!text.isNullOrBlank()) {
                        // 保留剪贴板中的完整原始分享文本
                        urlInput = text
                        isInputExpanded = true
                    }
                },
                onSubmit = {
                    if (urlInput.isNotBlank() && !loading) viewModel.submit(urlInput)
                }
            )

            // ── 2. 任务历史看板与状态过滤胶囊 ────────────────────────
            M3GlassCard(
                shape = RoundedCornerShape(18.dp),
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
                        historyLoading -> {
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
                                        modifier = Modifier.size(44.dp)
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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(historyItems, key = { it.id!! }) { task ->
                                    CompactTaskHistoryCard(
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
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.prevHistoryPage() },
                                enabled = historyPage > 1,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(stringResource(R.string.dm_prev_page), style = MaterialTheme.typography.labelSmall)
                            }

                            Text(
                                stringResource(R.string.dm_page_fmt, historyPage, historyTotalPages),
                                style = MaterialTheme.typography.labelSmall.tabularNumbers,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = Spacing.md)
                            )

                            OutlinedButton(
                                onClick = { viewModel.nextHistoryPage() },
                                enabled = historyPage < historyTotalPages,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(stringResource(R.string.dm_next_page), style = MaterialTheme.typography.labelSmall)
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
                    Text("删除任务记录", fontWeight = FontWeight.Bold)
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
                    enabled = !taskActionBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.onError)
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
    // activeColor 由调用方任意指定（含动态取色的 primary），固定白字在浅色 activeColor 上
    // 对比度不足。按背景亮度自动选取前景色，保证 >= 4.5:1。
    val activeContentColor = if (activeColor.luminance() > 0.5f) Color.Black else Color.White
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = modifier
            // 视觉高度保持 32dp，触摸目标扩至 48dp 下限（R6.5）
            .heightIn(min = 48.dp)
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeContentColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(2.dp))
            Surface(
                shape = CircleShape,
                color = if (selected) activeContentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.tabularNumbers,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) activeContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

// ── 顶部可折叠/紧凑智能解析舱 ────────────────────────────────────────

@Composable
private fun CollapsibleDownloadInputCard(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    loading: Boolean,
    autoReadClipboard: Boolean,
    onAutoClipboardToggle: (Boolean) -> Unit,
    onPasteClipboard: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            // 顶栏：紧凑标题、自动剪贴板开关与折叠展开箭头
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.bouncyClickable(onClick = onToggleExpand)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(XhsRed, Color(0xFFFF5E62)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "新建链接解析",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (autoReadClipboard) XhsRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.bouncyClickable { onAutoClipboardToggle(!autoReadClipboard) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (autoReadClipboard) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (autoReadClipboard) XhsRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "读剪贴板",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (autoReadClipboard) XhsRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (autoReadClipboard) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (!isExpanded && urlInput.isBlank()) {
                        IconButton(
                            onClick = onPasteClipboard,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.ContentPaste,
                                contentDescription = "粘贴",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 展开内容：输入框与提交按钮
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        placeholder = {
                            Text(
                                "粘贴小红书分享链接 (http://xhslink.com/...) 或完整文本",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        shape = RoundedCornerShape(12.dp),
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
                                modifier = Modifier.size(28.dp)
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

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (loading || urlInput.isBlank()) 0.dp else 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "正在解析...",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "加入下载解析",
                                        style = MaterialTheme.typography.labelMedium,
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
    }
}

// ── 精致任务记录卡片 (支持点击平滑展开查看完整错误详情与一键复制) ──

@Composable
private fun CompactTaskHistoryCard(
    task: XhsDownloadTaskVO,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val hasTitle = !task.workTitle.isNullOrBlank()
    val isSuccess = task.status == 1 || task.status == 3
    val isFailed = task.status == 2
    val isWaiting = task.status == 0

    // 是否展开查看完整错误或完整链接
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        border = BorderStroke(
            0.5.dp,
            if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable {
                // 点击卡片可在紧凑与展开详情之间切换
                if (isFailed || !hasTitle) {
                    isExpanded = !isExpanded
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 主展示行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：状态图标圆环
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when (task.status) {
                                1, 3 -> Color(0xFF3BAD7A).copy(alpha = 0.12f)
                                2 -> Color(0xFFE05252).copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (task.status) {
                            1 -> Icons.Filled.CheckCircle
                            3 -> Icons.Filled.Update
                            2 -> Icons.Filled.Cancel
                            else -> Icons.Filled.HourglassTop
                        },
                        contentDescription = null,
                        tint = when (task.status) {
                            1 -> Color(0xFF3BAD7A)
                            3 -> Color(0xFF4A90D9)
                            2 -> Color(0xFFE05252)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 中间：标题与状态时间
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val displayUrl = remember(task.url) {
                        DownloadManageViewModel.extractHttpUrl(task.url) ?: task.url ?: "无链接"
                    }
                    Text(
                        text = if (hasTitle) task.workTitle!! else displayUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (hasTitle) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.createTime ?: "",
                            style = MaterialTheme.typography.labelSmall.tabularNumbers,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        if (isFailed && !task.errorMessage.isNullOrBlank() && !isExpanded) {
                            Text(
                                text = "· ${task.errorMessage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 右侧：动作按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isFailed) {
                        FilledTonalButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("重试", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    } else if (isSuccess && !task.workId.isNullOrBlank()) {
                        Button(
                            onClick = { onViewDetail(task.workId!!) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("查看", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 删除/取消记录按钮：所有状态统一可用，触控热区升级为 36dp
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isWaiting) Icons.Filled.Close else Icons.Filled.DeleteOutline,
                            contentDescription = if (isWaiting) "取消任务" else "删除记录",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── 展开详情区 (如果展开且是失败任务，以红色背景气泡完整展示异常堆栈与一键复制) ──
            AnimatedVisibility(
                visible = isExpanded && isFailed && !task.errorMessage.isNullOrBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "失败原因详情",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // 复制完整原因
                            Text(
                                text = "复制原因",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(task.errorMessage ?: ""))
                                    Messenger.show("已复制失败原因到剪贴板")
                                }
                            )
                        }

                        Text(
                            text = task.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 16.sp
                        )

                        if (!task.url.isNullOrBlank() && task.url != task.workTitle) {
                            Text(
                                text = "原始链接: ${task.url}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
