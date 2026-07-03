package com.example.randomgallery.android.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupListViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<GroupVO>>(emptyList())
    val groups: StateFlow<List<GroupVO>> = _groups.asStateFlow()

    private val _pageInfo = MutableStateFlow("第 1 页")
    val pageInfo: StateFlow<String> = _pageInfo.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var page = 1
    private var totalPages = 1
    private var currentKeyword: String? = null

    fun query(keyword: String?, pageIndex: Int = 1) {
        page = pageIndex
        currentKeyword = keyword
        viewModelScope.launch {
            repository.getGroupList(keyword, page, 10)
                .onSuccess {
                    _groups.value = it.list
                    totalPages = it.pages
                    page = it.pageNum.toInt()
                    _pageInfo.value = "第 $page 页 / 共 $totalPages 页"
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "查询失败" }
        }
    }

    fun prevPage() {
        if (page > 1) query(currentKeyword, page - 1)
    }

    fun nextPage() {
        if (page < totalPages) query(currentKeyword, page + 1)
    }
}
