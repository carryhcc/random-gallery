package com.example.randomgallery.android.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.launch

class RandomGalleryViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _groups = MutableLiveData<List<GroupVO>>(emptyList())
    val groups: LiveData<List<GroupVO>> = _groups

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var page = 0
    private var hasMore = true

    fun refresh() {
        page = 0
        hasMore = true
        _groups.value = emptyList()
        loadMore(refresh = true)
    }

    fun loadMore(refresh: Boolean = false) {
        if (_loading.value == true || !hasMore) return
        _loading.value = true
        viewModelScope.launch {
            repository.loadMoreGroups(page = page, refresh = refresh)
                .onSuccess {
                    _groups.value = (_groups.value ?: emptyList()) + it.images
                    hasMore = it.hasMore
                    page += 1
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "加载失败" }
            _loading.value = false
        }
    }
}
