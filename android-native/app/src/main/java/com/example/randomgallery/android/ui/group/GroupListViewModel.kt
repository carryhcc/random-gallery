package com.example.randomgallery.android.ui.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.launch

class GroupListViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _groups = MutableLiveData<List<GroupVO>>(emptyList())
    val groups: LiveData<List<GroupVO>> = _groups

    private val _pageInfo = MutableLiveData("第 1 页")
    val pageInfo: LiveData<String> = _pageInfo

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

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
