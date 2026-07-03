package com.example.randomgallery.android.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RandomGalleryViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<GroupVO>>(emptyList())
    val groups: StateFlow<List<GroupVO>> = _groups.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var page = 0
    private var hasMore = true

    fun refresh() {
        page = 0
        hasMore = true
        _groups.value = emptyList()
        loadMore(refresh = true)
    }

    fun loadMore(refresh: Boolean = false) {
        if (_loading.value || !hasMore) return
        _loading.value = true
        viewModelScope.launch {
            repository.loadMoreGroups(page = page, refresh = refresh)
                .onSuccess {
                    _groups.value = _groups.value + it.images
                    hasMore = it.hasMore
                    page += 1
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "加载失败" }
            _loading.value = false
        }
    }
}
