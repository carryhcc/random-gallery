package com.example.randomgallery.android.ui.downloadlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.AuthorVO
import com.example.randomgallery.android.data.model.TagVO
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class DownloadListViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _works = MutableLiveData<List<XhsWorkListVO>>(emptyList())
    val works: LiveData<List<XhsWorkListVO>> = _works

    private val _authors = MutableLiveData<List<AuthorVO>>(emptyList())
    val authors: LiveData<List<AuthorVO>> = _authors

    private val _tags = MutableLiveData<List<TagVO>>(emptyList())
    val tags: LiveData<List<TagVO>> = _tags

    private val _viewMode = MutableLiveData("single")
    val viewMode: LiveData<String> = _viewMode

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var page = 1
    private var hasMore = true
    private var isLoadingInFlight = false  // 防止 snapshotFlow 同帧多次触发

    // 首屏是否已加载过。视图重建（从返回栈回到本页）时不再重复拉取，避免多余请求与内容被重新洗牌。
    var hasStarted = false

    var authorId: String? = null
    var tagId: Long? = null
    var keyword: String? = null
    private var seed: Int = Random.nextInt(1, 999999)

    fun init() {
        viewModelScope.launch {
            _viewMode.value = repository.viewModeFlow.first()
            repository.getAuthors().onSuccess { _authors.value = it }
            repository.getTags().onSuccess { _tags.value = it }
        }
    }

    fun changeViewMode(mode: String) {
        _viewMode.value = mode
        viewModelScope.launch { repository.saveViewMode(mode) }
    }

    fun resetFilters() {
        authorId = null
        tagId = null
        keyword = null
        refresh()
    }

    fun refresh() {
        page = 1
        hasMore = true
        isLoadingInFlight = false
        seed = Random.nextInt(1, 999999)
        _loading.value = false
        loadMore()
    }

    fun loadMore() {
        if (isLoadingInFlight || !hasMore) return
        isLoadingInFlight = true
        _loading.value = true
        viewModelScope.launch {
            val useSeed = if (authorId == null && tagId == null && keyword.isNullOrBlank()) seed else null
            repository.getWorkList(page, 10, authorId, tagId, keyword, useSeed)
                .onSuccess {
                    if (page == 1) {
                        _works.value = it.works
                    } else {
                        _works.value = (_works.value ?: emptyList()) + it.works
                    }
                    hasMore = it.hasMore
                    page += 1
                    _error.value = null
                }
                .onFailure {
                    _error.value = it.message ?: "加载失败"
                }
            _loading.value = false
            isLoadingInFlight = false
        }
    }
}
