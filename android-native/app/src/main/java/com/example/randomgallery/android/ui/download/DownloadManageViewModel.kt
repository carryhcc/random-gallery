package com.example.randomgallery.android.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DownloadManageViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

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
            repository.autoReadClipboardFlow.collectLatest { _autoReadClipboard.value = it }
        }
        // 首次加载历史 + 每 3 秒轮询刷新进度（VM 随返回栈销毁，页面退出即停止）
        viewModelScope.launch {
            while (true) {
                loadHistory(showLoading = false)
                delay(3000)
            }
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
                val result = repository.addDownloadTask(resolvedUrl)
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
        viewModelScope.launch { repository.saveAutoReadClipboard(enabled) }
    }

    // ── 历史操作 ──

    // 避免轮询与用户翻页并发加载互相覆盖：新请求会取消上一次进行中的请求
    private var historyJob: Job? = null

    fun loadHistory(showLoading: Boolean = true) {
        historyJob?.cancel()
        if (showLoading) _historyLoading.value = true
        historyJob = viewModelScope.launch {
            try {
                val page = _historyPage.value
                val result = repository.getDownloadHistory(page, HISTORY_PAGE_SIZE)
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
            } finally {
                _historyLoading.value = false
            }
        }
    }

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
            val result = repository.retryDownloadTask(id)
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
