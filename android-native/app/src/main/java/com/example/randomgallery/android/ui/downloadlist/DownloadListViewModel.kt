package com.example.randomgallery.android.ui.downloadlist

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.data.model.AuthorVO
import com.example.randomgallery.android.data.model.TagVO
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class DownloadListViewModel(
    private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)

    private val _works = MutableStateFlow<List<XhsWorkListVO>>(emptyList())
    val works: StateFlow<List<XhsWorkListVO>> = _works.asStateFlow()

    private val _authors = MutableStateFlow<List<AuthorVO>>(emptyList())
    val authors: StateFlow<List<AuthorVO>> = _authors.asStateFlow()

    private val _tags = MutableStateFlow<List<TagVO>>(emptyList())
    val tags: StateFlow<List<TagVO>> = _tags.asStateFlow()

    private val _viewMode = MutableStateFlow("single")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var page = 1
    private var hasMore = true
    private var isLoadingInFlight = false  // 防止 snapshotFlow 同帧多次触发

    var authorId: String? = null
    var tagId: Long? = null
    var keyword: String? = null
    private var seed: Int = Random.nextInt(1, 999999)

    init {
        authorId = savedStateHandle.get<String>("filterAuthorId")
        keyword = savedStateHandle.get<String>("filterKeyword")
        init()
        loadMore()
    }

    fun init() {
        viewModelScope.launch {
            _viewMode.value = repository().viewModeFlow.first()
            repository().getAuthors().onSuccess { _authors.value = it }
            repository().getTags().onSuccess { _tags.value = it }
        }
    }

    fun changeViewMode(mode: String) {
        _viewMode.value = mode
        viewModelScope.launch { repository().saveViewMode(mode) }
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
            repository().getWorkList(page, 10, authorId, tagId, keyword, useSeed)
                .onSuccess {
                    val merged = if (page == 1) it.works else _works.value + it.works
                    // 上界截断：长期会话不无界增长
                    _works.value = merged.take(MAX_WORKS)
                    hasMore = it.hasMore && merged.size < MAX_WORKS
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

    private companion object {
        const val MAX_WORKS = 200
    }
}
