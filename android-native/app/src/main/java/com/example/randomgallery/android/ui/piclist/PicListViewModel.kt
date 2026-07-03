package com.example.randomgallery.android.ui.piclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PicListViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<PicVO>>(emptyList())
    val items: StateFlow<List<PicVO>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 1
    private var hasMore = true
    var groupId: Long = 0

    fun refresh() {
        currentPage = 1
        hasMore = true
        _items.value = emptyList()
        loadMore()
    }

    fun loadMore() {
        if (_loading.value || !hasMore || groupId == 0L) return
        _loading.value = true
        viewModelScope.launch {
            repository.getPicList(groupId = groupId, page = currentPage, size = 10)
                .onSuccess {
                    val merged = _items.value + it
                    _items.value = merged
                    hasMore = it.size >= 10
                    currentPage += 1
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "加载失败" }
            _loading.value = false
        }
    }
}
