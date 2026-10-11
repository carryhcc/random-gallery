package com.example.randomgallery.android.ui.download

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val XHS_LOGIN_URL = "https://www.xiaohongshu.com/explore"

/** 只从小红书 HTTPS 页面读取 Cookie，不向网页暴露原生桥接。 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XhsCookieLoginDialog(onDismiss: () -> Unit, onCookie: (String) -> Unit) {
    var browser by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler { if (browser?.canGoBack() == true) browser?.goBack() else onDismiss() }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    TextButton(onClick = { browser?.reload() }) { Text("刷新") }
                }
                Text("登录小红书后，点击底部按钮获取 Cookie。", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { context ->
                        WebView(context).apply {
                            browser = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            // 使用桌面登录页面，避免移动站只显示“打开 App”。
                            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val uri = request.url
                                    val host = uri.host.orEmpty().lowercase()
                                    return uri.scheme != "https" || (host != "xiaohongshu.com" && !host.endsWith(".xiaohongshu.com"))
                                }
                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    loading = true
                                    error = null
                                }
                                override fun onPageFinished(view: WebView, url: String?) { loading = false }
                                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, failure: SslError) {
                                    handler.cancel()
                                    loading = false
                                    error = "登录页面证书校验失败，请检查网络，或使用手动更新。"
                                }
                                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                    if (request.isForMainFrame) {
                                        loading = false
                                        error = "登录页面暂不可用，请刷新或使用手动更新。"
                                    }
                                }
                                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                    if (request.isForMainFrame) {
                                        loading = false
                                        error = "登录页面加载失败，请检查网络后刷新，或使用手动更新。"
                                    }
                                }
                            }
                            loadUrl(XHS_LOGIN_URL)
                        }
                    },
                    onRelease = { view ->
                        browser = null
                        view.stopLoading()
                        view.destroy()
                    }
                )
                Button(
                    onClick = {
                        val cookie = CookieManager.getInstance().getCookie(XHS_LOGIN_URL).orEmpty()
                        val hasSession = cookie.split(';').any {
                            val pair = it.trim().split('=', limit = 2)
                            pair.size == 2 && pair[0] == "web_session" && pair[1].isNotBlank()
                        }
                        if (hasSession && DownloadManageViewModel.isCookieFormatPlausible(cookie)) {
                            CookieManager.getInstance().flush()
                            onCookie(cookie)
                        } else {
                            error = "尚未获取到登录 Cookie，请先完成网页登录。"
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("获取 Cookie 并校验") }
            }
        }
    }
}
