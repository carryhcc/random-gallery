package com.example.randomgallery.android.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.di.RepositoryProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.SingleFlight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class CookieStatus { EMPTY, SAVED, CHECKING, VALID, INVALID }

@HiltViewModel
class DownloadManageViewModel @Inject constructor(
    private val repositories: RepositoryProvider,
) : ViewModel() {

    private fun repository(): GalleryRepository = repositories.repository()

    private val _submitEvents = Channel<Result<String>>(Channel.BUFFERED)
    val submitEvents: Flow<Result<String>> = _submitEvents.receiveAsFlow()

    // 历史列表操作结果（重试等）的提示消息，独立于 submit 事件，避免误清空输入框
    private val _historyEvents = Channel<Result<String>>(Channel.BUFFERED)
    val historyEvents: Flow<Result<String>> = _historyEvents.receiveAsFlow()

    // 最近一次成功解析的 URL，用于在 Snackbar 展示
    private val _lastResolvedUrl = MutableStateFlow<String?>(null)
    val lastResolvedUrl: StateFlow<String?> = _lastResolvedUrl.asStateFlow()

    // 写操作 in-flight 忙碌锁：防止删除/重试请求窗口内连点重复提交
    private val taskActionFlight = SingleFlight()

    /** 删除任务/重试任务进行中。UI 据此禁用确认按钮。 */
    val taskActionBusy: StateFlow<Boolean> = taskActionFlight.busy

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _autoReadClipboard = MutableStateFlow(false)
    val autoReadClipboard: StateFlow<Boolean> = _autoReadClipboard.asStateFlow()

    // ── XHS Cookie 保存与校验 ──
    private val _xhsCookie = MutableStateFlow("")
    val xhsCookie: StateFlow<String> = _xhsCookie.asStateFlow()

    private val _cookieStatus = MutableStateFlow(CookieStatus.EMPTY)
    val cookieStatus: StateFlow<CookieStatus> = _cookieStatus.asStateFlow()

    private val _cookieEvents = Channel<Result<String>>(Channel.BUFFERED)
    val cookieEvents: Flow<Result<String>> = _cookieEvents.receiveAsFlow()

    private var cookieValidationJob: Job? = null
    private val _cookieSaving = MutableStateFlow(false)
    val cookieSaving: StateFlow<Boolean> = _cookieSaving.asStateFlow()

    // ── 下载历史 ──
    private val HISTORY_PAGE_SIZE = 10

    // 状态筛选：null-全部, 0-等待/进行中, 1-已完成, 2-失败
    private val _statusFilter = MutableStateFlow<Int?>(null)
    val statusFilter: StateFlow<Int?> = _statusFilter.asStateFlow()

    private val _stats = MutableStateFlow(com.example.randomgallery.android.data.model.DownloadTaskStatsVO())
    val stats: StateFlow<com.example.randomgallery.android.data.model.DownloadTaskStatsVO> = _stats.asStateFlow()

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
        viewModelScope.launch {
            repository().xhsCookieFlow.collectLatest { saved ->
                if (saved != _xhsCookie.value) {
                    _xhsCookie.value = saved
                    _cookieStatus.value = if (saved.isBlank()) CookieStatus.EMPTY else CookieStatus.SAVED
                }
            }
        }
    }

    fun saveCookie(raw: String, validateAfterSave: Boolean = false) {
        if (_cookieSaving.value) return
        val trimmed = raw.trim()
        if (trimmed.isNotBlank() && !isCookieFormatPlausible(trimmed)) {
            _cookieEvents.trySend(Result.failure(Exception("Cookie 格式不正确")))
            return
        }
        _cookieSaving.value = true
        cookieValidationJob?.cancel()
        _cookieStatus.value = if (_xhsCookie.value.isBlank()) CookieStatus.EMPTY else CookieStatus.SAVED
        viewModelScope.launch {
            try {
                repository().saveXhsCookie(trimmed)
                _xhsCookie.value = trimmed
                _cookieStatus.value = if (trimmed.isBlank()) CookieStatus.EMPTY else CookieStatus.SAVED
                _cookieEvents.trySend(Result.success(if (trimmed.isBlank()) "已清除 Cookie" else "Cookie 已更新"))
                if (validateAfterSave && trimmed.isNotBlank()) validateCookie()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _cookieEvents.trySend(Result.failure(Exception("Cookie 保存失败，请重试")))
            } finally {
                _cookieSaving.value = false
            }
        }
    }

    fun validateCookie() {
        val cookie = _xhsCookie.value
        if (cookie.isBlank()) {
            _cookieEvents.trySend(Result.failure(Exception("请先保存 Cookie")))
            return
        }
        if (_cookieStatus.value == CookieStatus.CHECKING) return
        _cookieStatus.value = CookieStatus.CHECKING
        cookieValidationJob = viewModelScope.launch {
            val r = repository().validateXhsCookie(cookie)
            // Cookie 被外部更新时，旧响应也不能覆盖新值的状态。
            if (_xhsCookie.value != cookie) return@launch
            when {
                r.isSuccess && r.getOrNull() == true -> {
                    _cookieStatus.value = CookieStatus.VALID
                    _cookieEvents.trySend(Result.success("校验通过"))
                }
                r.isSuccess -> {
                    _cookieStatus.value = CookieStatus.INVALID
                    _cookieEvents.trySend(Result.failure(Exception("Cookie 已失效，或探测作品无法访问，请重新获取")))
                }
                else -> {
                    _cookieStatus.value = CookieStatus.SAVED
                    _cookieEvents.trySend(Result.failure(r.exceptionOrNull() ?: Exception("校验失败")))
                }
            }
        }
    }

    fun submit(url: String) {
        if (_loading.value) return          // 防止重复提交
        if (_cookieSaving.value) {
            _submitEvents.trySend(Result.failure(Exception("Cookie 正在保存，请稍后入库")))
            return
        }
        val trimmedInput = url.trim()
        if (trimmedInput.isBlank()) {
            _submitEvents.trySend(Result.failure(Exception("请输入链接")))
            return
        }
        val resolvedUrl = extractHttpUrl(trimmedInput) ?: trimmedInput
        _loading.value = true
        _lastResolvedUrl.value = resolvedUrl
        viewModelScope.launch {
            try {
                // 提交用户输入的原始完整文本（保留中文分享语与链接），后端负责提取并留存原始 params_url
                val result = repository().addDownloadTask(trimmedInput)
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
    private var pollConsecutiveErrors = 0

    fun setStatusFilter(status: Int?) {
        if (_statusFilter.value == status) return
        _statusFilter.value = status
        _historyPage.value = 1
        _history.value = emptyList() // 立即清空旧数据，瞬间触发加载动画/骨架状态
        loadHistory(showLoading = true, allowWhileBusy = true)
    }

    fun loadStats() {
        viewModelScope.launch {
            repository().getDownloadStats().onSuccess {
                _stats.value = it
            }
        }
    }

    fun deleteTask(id: Long) {
        taskActionFlight.launch(viewModelScope) {
            val result = repository().deleteDownloadTask(id)
            _historyEvents.trySend(result)
            if (result.isSuccess) {
                loadHistory(showLoading = false)
                loadStats()
            }
        }
    }

    fun loadHistory(showLoading: Boolean = true, allowWhileBusy: Boolean = true) {
        if (historyRequesting && !allowWhileBusy) return
        if (!allowWhileBusy && pollConsecutiveErrors >= 5) return  // 连续失败 5 次熔断，避免死循环轮询
        historyRequesting = true
        loadStats()
        val version = ++historyVersion
        if (showLoading) _historyLoading.value = true
        viewModelScope.launch {
            try {
                val page = _historyPage.value
                val filter = _statusFilter.value
                val result = repository().getDownloadHistory(page, HISTORY_PAGE_SIZE, filter)
                if (version == historyVersion) {
                    if (result.isSuccess) {
                        val data = result.getOrNull()
                        if (data != null) {
                            _history.value = data.list
                            _historyTotalPages.value = data.pages
                            _historyError.value = null
                            pollConsecutiveErrors = 0
                            if (data.list.isEmpty() && page > 1 && data.pages in 1 until page) {
                                _historyPage.value = data.pages
                                loadHistory(showLoading = false)
                            }
                        }
                    } else {
                        pollConsecutiveErrors++
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
        taskActionFlight.launch(viewModelScope) {
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

        fun isCookieFormatPlausible(raw: String?): Boolean {
            val value = raw?.trim() ?: return false
            if (value.isBlank()) return false
            if (value.length !in 16..8192) return false
            if (value.any { it.code < 32 || it.code >= 127 }) return false
            return value.split(';').all { part ->
                val pair = part.trim().split('=', limit = 2)
                pair.size == 2 && pair[0].isNotBlank() && pair[0].none { it.isWhitespace() }
            }
        }
    }
}
