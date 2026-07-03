package com.example.randomgallery.android.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: GalleryRepository
) : ViewModel() {

    private val _submitEvents = Channel<Result<String>>(Channel.BUFFERED)
    val submitEvents: Flow<Result<String>> = _submitEvents.receiveAsFlow()

    // 最近一次成功解析的 URL，用于在 Snackbar 展示
    private val _lastResolvedUrl = MutableStateFlow<String?>(null)
    val lastResolvedUrl: StateFlow<String?> = _lastResolvedUrl.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _autoReadClipboard = MutableStateFlow(false)
    val autoReadClipboard: StateFlow<Boolean> = _autoReadClipboard.asStateFlow()

    init {
        viewModelScope.launch {
            repository.autoReadClipboardFlow.collectLatest { _autoReadClipboard.value = it }
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
                _submitEvents.trySend(repository.addDownloadTask(resolvedUrl))
            } finally {
                _loading.value = false
            }
        }
    }

    fun setAutoReadClipboard(enabled: Boolean) {
        _autoReadClipboard.value = enabled
        viewModelScope.launch { repository.saveAutoReadClipboard(enabled) }
    }

    companion object {
        private val HTTP_URL_PATTERN = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

        fun extractHttpUrl(text: String?): String? {
            if (text.isNullOrBlank()) return null
            return HTTP_URL_PATTERN.find(text)?.value
        }
    }
}
