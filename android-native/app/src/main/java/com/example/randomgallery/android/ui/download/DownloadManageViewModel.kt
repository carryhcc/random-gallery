package com.example.randomgallery.android.ui.download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DownloadManageViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)

    private val _submitEvents = Channel<Result<String>>(Channel.BUFFERED)
    val submitEvents: Flow<Result<String>> = _submitEvents.receiveAsFlow()

    // 历史列表操作结果（重试等）的提示消息，独立于 submit 事件，避免误清空输入框
    private val _historyEvents = Channel<Result<String>>(Channel.BUFFERED)
    val historyEvents: Flow<Result<String>> = _historyEvents.receiveAsFlow()

    // 最近一次成功解析的 URL，用于在 Snackbar 展示
    private val _lastResolvedUrl = MutableStateFlow<String?>(null)
    val lastResolvedUrl: StateFlow<String?> = _lastResolvedUrl.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _autoReadClipboard = MutableStateFlow(false)
    val autoReadClipboard: StateFlow<Boolean> = _autoReadClipboard.asStateFlow()

    // ── 下载历史 ──
    private val HISTORY_PAGE_SIZE = 10

    private val _history = MutableStateFlow<List<XhsDownloadTaskVO>>(emptyList())
    val history: StateFlow<List<XhsDownloadTaskVO>> = _history.asStateFlow()

    private val _historyPage = MutableStateFlow(1)
    val historyPage: StateFlow<Int> = _historyPage.asStateFlow()

    private val _historyTotalPages = MutableStateFlow(0)
    val historyTotalPages: StateFlow<Int> = _historyTotalPages.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    init {
        viewModelScope.launch {
            repository().autoReadClipboardFlow.collectLatest { _autoReadClipboard.value = it }
        }
    }

    fun submit(url: String) {
        if (_loading.value) return          // 防止重复提交
        val resolvedUrl = extractHttpUrl(url) ?: url.trim()
        if (resolvedUrl.isBlank()) {
            _submitEvents.trySend(Result.failure(Exception("请输入链接")))
            return
        }
        _loading.value = true
        _lastResolvedUrl.value = resolvedUrl
        viewModelScope.launch {
            try {
                val result = repository().addDownloadTask(resolvedUrl)
                _submitEvents.trySend(result)
                if (result.isSuccess) {
                    // 添加成功后回到第一页查看最新记录
                    _historyPage.value = 1
                    loadHistory(showLoading = false)
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun setAutoReadClipboard(enabled: Boolean) {
        _autoReadClipboard.value = enabled
        viewModelScope.launch { repository().saveAutoReadClipboard(enabled) }
    }

    // ── 历史操作 ──

    // 版本号防乱序：并发请求中只采纳最新的结果，避免旧响应覆盖新响应；
    // 不再取消在途请求，消除「轮询每 3s 取消上一个未完成请求」造成的饥饿与耗电。
    private var historyVersion = 0

    // 是否有请求在途：轮询时若上一请求未完成则跳过本 tick，避免并发堆积
    private var historyRequesting = false

    fun loadHistory(showLoading: Boolean = true, allowWhileBusy: Boolean = true) {
        if (historyRequesting && !allowWhileBusy) return
        historyRequesting = true
        val version = ++historyVersion
        if (showLoading) _historyLoading.value = true
        viewModelScope.launch {
            try {
                val page = _historyPage.value
                val result = repository().getDownloadHistory(page, HISTORY_PAGE_SIZE)
                if (version == historyVersion) {
                    if (result.isSuccess) {
                        val data = result.getOrNull()
                        if (data != null) {
                            _history.value = data.list
                            _historyTotalPages.value = data.pages
                            _historyError.value = null
                        }
                    } else {
                        _historyError.value = result.exceptionOrNull()?.message
                    }
                }
            } finally {
                historyRequesting = false
                if (showLoading) _historyLoading.value = false
            }
        }
    }

    /** 由页面生命周期驱动（页面可见时才轮询）：每 tick 仅当没有在途请求时刷新 */
    fun pollHistory() = loadHistory(showLoading = false, allowWhileBusy = false)

    fun refreshHistory() {
        _historyPage.value = 1
        loadHistory()
    }

    fun nextHistoryPage() {
        if (_historyPage.value >= _historyTotalPages.value) return
        _historyPage.value += 1
        loadHistory()
    }

    fun prevHistoryPage() {
        if (_historyPage.value <= 1) return
        _historyPage.value -= 1
        loadHistory()
    }

    fun retryTask(id: Long) {
        viewModelScope.launch {
            val result = repository().retryDownloadTask(id)
            _historyEvents.trySend(result)
            if (result.isSuccess) {
                loadHistory(showLoading = false)
            }
        }
    }

    companion object {
        private val HTTP_URL_PATTERN = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

        fun extractHttpUrl(text: String?): String? {
            if (text.isNullOrBlank()) return null
            return HTTP_URL_PATTERN.find(text)?.value
        }
    }
}
