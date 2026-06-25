package com.example.randomgallery.android.ui.download

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.view.ViewTreeObserver
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*

@Composable
fun DownloadManageScreen(
    viewModel: DownloadManageViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val loading by viewModel.loading.observeAsState(false)
    val submitResult by viewModel.submitResult.observeAsState()
    val lastResolvedUrl by viewModel.lastResolvedUrl.observeAsState()
    val autoReadClipboard by viewModel.autoReadClipboard.observeAsState(false)

    var urlInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(submitResult) {
        submitResult?.let { result ->
            val msg = if (result.isSuccess) {
                val urlHint = lastResolvedUrl?.let { url ->
                    val short = if (url.length > 40) "…${url.takeLast(30)}" else url
                    "\n$short"
                } ?: ""
                "${result.getOrDefault("提交成功")}$urlHint"
            } else {
                result.exceptionOrNull()?.message ?: "提交失败"
            }
            snackbarHostState.showSnackbar(msg)
            if (result.isSuccess) urlInput = ""
        }
    }

    TopSnackbarBox(snackbarHostState) {
    Scaffold(
        containerColor = FeedBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { XhsTopBar(title = "下载管理", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(FeedBackground)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text("添加下载链接", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("小红书链接") },
                        placeholder = { Text("粘贴分享链接 …", color = TextSecondary) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XhsRed),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                                val extracted = DownloadManageViewModel.extractHttpUrl(text)
                                if (!extracted.isNullOrBlank()) urlInput = extracted
                            }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "粘贴", tint = XhsRed)
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
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = NeutralWhite, strokeWidth = 2.dp)
                        } else {
                            Text("提交下载", fontWeight = FontWeight.Bold)
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
                        Text("自动读取剪贴板", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("进入页面时自动粘贴链接", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = autoReadClipboard,
                        onCheckedChange = { viewModel.setAutoReadClipboard(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = XhsRed, checkedTrackColor = XhsRedSoft)
                    )
                }
            }
        }
    }
    } // end TopSnackbarBox
}
