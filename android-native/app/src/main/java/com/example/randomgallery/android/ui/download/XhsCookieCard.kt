package com.example.randomgallery.android.ui.download

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.randomgallery.android.ui.common.M3GlassCard
import com.example.randomgallery.android.ui.theme.Spacing

@Composable
fun XhsCookieCard(viewModel: DownloadManageViewModel) {
    val cookie by viewModel.xhsCookie.collectAsStateWithLifecycle()
    val status by viewModel.cookieStatus.collectAsStateWithLifecycle()
    val saving by viewModel.cookieSaving.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var loggingIn by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current
    val busy = saving || status == CookieStatus.CHECKING

    M3GlassCard(
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("小红书 Cookie", style = MaterialTheme.typography.titleSmall)
            Text(
                when (status) {
                    CookieStatus.EMPTY -> "未设置 · 入库使用服务端默认 Cookie"
                    CookieStatus.SAVED -> "已保存 · 尚未校验"
                    CookieStatus.CHECKING -> "正在校验…"
                    CookieStatus.VALID -> "校验通过 · 新入库任务自动携带"
                    CookieStatus.INVALID -> "校验未通过 · 请重新获取或更新"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (status == CookieStatus.INVALID) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            // 窄屏自动换行，避免操作按钮挤出页面。
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { loggingIn = true }, enabled = !busy) { Text("登录获取") }
                TextButton(onClick = { draft = cookie; editing = true }, enabled = !busy) { Text("手动更新") }
                TextButton(onClick = viewModel::validateCookie, enabled = !busy && cookie.isNotBlank()) { Text("校验") }
                TextButton(onClick = { viewModel.saveCookie("") }, enabled = !busy && cookie.isNotBlank()) { Text("清除") }
            }
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { if (!saving) editing = false },
            title = { Text("更新小红书 Cookie") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("粘贴网页登录后的完整 Cookie。保存后自动校验，新入库任务将携带更新值。")
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Cookie") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        enabled = !saving
                    )
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        draft = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    }) { Text("从剪贴板粘贴") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveCookie(draft, validateAfterSave = true)
                    editing = false
                }, enabled = !saving && DownloadManageViewModel.isCookieFormatPlausible(draft)) { Text("保存并校验") }
            },
            dismissButton = { TextButton(onClick = { editing = false }, enabled = !saving) { Text("取消") } }
        )
    }
    if (loggingIn) {
        XhsCookieLoginDialog(
            onDismiss = { loggingIn = false },
            onCookie = {
                viewModel.saveCookie(it, validateAfterSave = true)
                loggingIn = false
            }
        )
    }
}
