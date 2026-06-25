package com.example.randomgallery.android.ui.download

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.SingleLiveEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadManageViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _submitResult = SingleLiveEvent<Result<String>>()
    val submitResult: LiveData<Result<String>> = _submitResult

    // 最近一次成功解析的 URL，用于在 Snackbar 展示
    private val _lastResolvedUrl = MutableLiveData<String?>()
    val lastResolvedUrl: LiveData<String?> = _lastResolvedUrl

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _autoReadClipboard = MutableLiveData(false)
    val autoReadClipboard: LiveData<Boolean> = _autoReadClipboard

    init {
        viewModelScope.launch {
            repository.autoReadClipboardFlow.collectLatest { _autoReadClipboard.value = it }
        }
    }

    fun submit(url: String) {
        if (_loading.value == true) return          // 防止重复提交
        val resolvedUrl = extractHttpUrl(url) ?: url.trim()
        if (resolvedUrl.isBlank()) {
            _submitResult.value = Result.failure(Exception("请输入链接"))
            return
        }
        _loading.value = true
        _lastResolvedUrl.value = resolvedUrl
        viewModelScope.launch {
            try {
                _submitResult.value = repository.addDownloadTask(resolvedUrl)
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
