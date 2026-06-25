package com.example.randomgallery.android.ui.piclist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.launch

class PicListViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _items = MutableLiveData<List<PicVO>>(emptyList())
    val items: LiveData<List<PicVO>> = _items

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

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
        if (_loading.value == true || !hasMore || groupId == 0L) return
        _loading.value = true
        viewModelScope.launch {
            repository.getPicList(groupId = groupId, page = currentPage, size = 10)
                .onSuccess {
                    val merged = (_items.value ?: emptyList()) + it
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
