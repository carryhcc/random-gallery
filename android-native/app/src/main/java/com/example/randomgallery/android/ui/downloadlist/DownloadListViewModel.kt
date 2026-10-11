package com.example.randomgallery.android.ui.downloadlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.di.RepositoryProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.randomgallery.android.data.model.AuthorVO
import com.example.randomgallery.android.data.model.TagVO
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

@HiltViewModel
class DownloadListViewModel @Inject constructor(
    private val repositories: RepositoryProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private fun repository(): GalleryRepository = repositories.repository()

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

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 以下分页/筛选状态一律走 SavedStateHandle：原先它们是普通字段，只有
    // authorId / keyword 进过 handle，于是旋转屏幕后 page 回到 1、seed 重新随机，
    // 列表会以新顺序整体重排，用户看到的已是另一份数据。isLoadingInFlight 则是
    // 纯粹的瞬时在途标记，重建后必然无在途请求，不需要保存。
    private var page: Int
        get() = savedStateHandle[KEY_PAGE] ?: 1
        set(value) { savedStateHandle[KEY_PAGE] = value }

    private var hasMore: Boolean
        get() = savedStateHandle[KEY_HAS_MORE] ?: true
        set(value) { savedStateHandle[KEY_HAS_MORE] = value }

    private var isLoadingInFlight = false  // 防止 snapshotFlow 同帧多次触发

    var authorId: String?
        get() = savedStateHandle[KEY_AUTHOR_ID]
        set(value) { savedStateHandle[KEY_AUTHOR_ID] = value }

    var tagId: Long?
        get() = savedStateHandle[KEY_TAG_ID]
        set(value) { savedStateHandle[KEY_TAG_ID] = value }

    var keyword: String?
        get() = savedStateHandle[KEY_KEYWORD]
        set(value) { savedStateHandle[KEY_KEYWORD] = value }

    /** 随机排序种子：必须持久化，否则重建后列表顺序会变 */
    private var seed: Int
        get() = savedStateHandle[KEY_SEED] ?: Random.nextInt(1, 999999).also { savedStateHandle[KEY_SEED] = it }
        set(value) { savedStateHandle[KEY_SEED] = value }

    init {
        // 导航参数优先于已保存值：用户从筛选入口进来时应覆盖上次的选择
        savedStateHandle.get<String>("filterAuthorId")?.let { authorId = it }
        savedStateHandle.get<String>("filterKeyword")?.let { keyword = it }
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

    fun removeWork(workId: String) {
        _works.value = _works.value.filter { it.workId != workId }
    }

    fun refresh() {
        page = 1
        hasMore = true
        isLoadingInFlight = false
        seed = Random.nextInt(1, 999999)
        _loading.value = false
        loadMore()
    }

    /** 下拉刷新：重置列表并同时刷新作者/标签筛选数据 */
    fun refreshAll() {
        if (_refreshing.value) return
        _refreshing.value = true
        page = 1
        hasMore = true
        seed = Random.nextInt(1, 999999)
        isLoadingInFlight = true
        viewModelScope.launch {
            val worksDeferred = async {
                repository().getWorkList(1, 10, authorId, tagId, keyword, seed)
            }
            val authorsDeferred = async { repository().getAuthors() }
            val tagsDeferred = async { repository().getTags() }
            worksDeferred.await()
                .onSuccess {
                    _works.value = it.works.take(MAX_WORKS)
                    hasMore = it.hasMore && it.works.size < MAX_WORKS
                    page = 2
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "加载失败" }
            authorsDeferred.await().onSuccess { _authors.value = it }
            tagsDeferred.await().onSuccess { _tags.value = it }
            _refreshing.value = false
            isLoadingInFlight = false
        }
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

        const val KEY_PAGE = "dlPage"
        const val KEY_HAS_MORE = "dlHasMore"
        const val KEY_SEED = "dlSeed"
        const val KEY_AUTHOR_ID = "dlAuthorId"
        const val KEY_TAG_ID = "dlTagId"
        const val KEY_KEYWORD = "dlKeyword"
    }
}
