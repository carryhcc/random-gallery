package com.example.randomgallery.android.ui.download

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.ViewTreeObserver
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*

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

    val history by viewModel.history.collectAsStateWithLifecycle()
    val historyPage by viewModel.historyPage.collectAsStateWithLifecycle()
    val historyTotalPages by viewModel.historyTotalPages.collectAsStateWithLifecycle()
    val historyLoading by viewModel.historyLoading.collectAsStateWithLifecycle()
    val historyError by viewModel.historyError.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }

    // Android 12+ 只允许在窗口获焦时读剪贴板，用 ViewTreeObserver 监听窗口焦点
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

    // 历史列表操作结果（重试等）提示，不影响输入框内容
    LaunchedEffect(Unit) {
        viewModel.historyEvents.collect { result ->
            val msg = result.exceptionOrNull()?.message ?: result.getOrDefault("操作成功")
            Messenger.show(msg, isError = result.isFailure)
        }
    }

    // 页面处于 STARTED 状态时才轮询刷新下载进度：离开页面/App 退后台即自动停止；
    // pollHistory 内部会在有在途请求时跳过，避免每 3s 取消上一个请求造成饥饿。
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { XhsTopBar(title = stringResource(R.string.dm_title), onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(stringResource(R.string.dm_add_link), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text(stringResource(R.string.dm_link_label)) },
                        placeholder = { Text(stringResource(R.string.dm_link_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                                val extracted = DownloadManageViewModel.extractHttpUrl(text)
                                if (!extracted.isNullOrBlank()) urlInput = extracted
                            }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.dm_paste), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                    Button(
                        onClick = { viewModel.submit(urlInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.dm_submit), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.dm_auto_clipboard), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.dm_auto_clipboard_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoReadClipboard,
                        onCheckedChange = { viewModel.setAutoReadClipboard(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            // ── 下载历史 ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = Spacing.xl, end = Spacing.sm, top = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.dm_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { viewModel.refreshHistory() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.common_refresh), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (history.isEmpty() && !historyLoading && historyError == null) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.dm_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (history.isEmpty() && historyLoading) {
                        XhsLoadingBox(Modifier.weight(1f).fillMaxWidth())
                    } else {
                        val historyItems = history.filter { it.id != null }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)
                        ) {
                            items(historyItems, key = { it.id!! }) { task ->
                                HistoryItem(
                                    task = task,
                                    onRetry = { viewModel.retryTask(task.id!!) },
                                    onViewDetail = { workId -> onViewDetail(workId) }
                                )
                            }
                        }
                    }
                    if (historyTotalPages > 1) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.md),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.prevHistoryPage() },
                                enabled = historyPage > 1,
                                contentPadding = PaddingValues(horizontal = Spacing.md)
                            ) {
                                Text(stringResource(R.string.dm_prev_page))
                            }
                            Text(
                                "第 $historyPage / $historyTotalPages 页",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.md)
                            )
                            OutlinedButton(
                                onClick = { viewModel.nextHistoryPage() },
                                enabled = historyPage < historyTotalPages,
                                contentPadding = PaddingValues(horizontal = Spacing.md)
                            ) {
                                Text(stringResource(R.string.dm_next_page))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    task: XhsDownloadTaskVO,
    onRetry: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val hasTitle = !task.workTitle.isNullOrBlank()
            // 主行：已完成任务优先展示作品标题，否则展示链接
            Text(
                if (hasTitle) task.workTitle else task.url ?: "",
                style = if (hasTitle) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (hasTitle) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 副行：有标题时展示原始链接，避免丢失来源信息
            if (hasTitle && task.url != null && task.url != task.workTitle) {
                Text(
                    task.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(task)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    task.createTime ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.status == 2 && !task.errorMessage.isNullOrBlank()) {
                Text(
                    task.errorMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (task.status == 2) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = Spacing.sm)) {
                Text(stringResource(R.string.dm_retry), color = MaterialTheme.colorScheme.error)
            }
        }
        if ((task.status == 1 || task.status == 3) && !task.workId.isNullOrBlank()) {
            TextButton(
                onClick = { onViewDetail(task.workId!!) },
                contentPadding = PaddingValues(horizontal = Spacing.sm)
            ) {
                Text(stringResource(R.string.dm_view_detail), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StatusBadge(task: XhsDownloadTaskVO) {
    val (text, color) = when (task.status) {
        0 -> stringResource(R.string.dm_status_waiting) to MaterialTheme.colorScheme.primary
        1 -> stringResource(R.string.dm_status_completed) to AccentGreen
        3 -> stringResource(R.string.dm_status_updated) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(R.string.dm_status_failed) to MaterialTheme.colorScheme.error
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = color
    )
}
