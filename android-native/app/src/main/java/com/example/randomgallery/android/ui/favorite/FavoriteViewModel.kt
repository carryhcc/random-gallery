package com.example.randomgallery.android.ui.favorite

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.data.model.RandomGifVO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 收藏列表（REQ-01）。
 *
 * 复用后端已有的 `GET /api/xhsWork/favorite/list`（按收藏时间倒序、分页）。
 * 在此之前 Android 端只有"切换收藏"的写入口，没有任何读出口，收藏结果无处可见。
 */
class FavoriteViewModel(
    private val loadFavorites: suspend (page: Int, size: Int) -> Result<List<RandomGifVO>>,
    private val toggleFavorite: suspend (id: Long, type: String) -> Result<Boolean>
) : ViewModel() {

    /** 生产环境构造器：从 AppContainer 取单例仓库。 */
    constructor(appContext: Context) : this(
        loadFavorites = { page, size -> AppContainer.repository(appContext).getFavorites(page, size) },
        toggleFavorite = { id, type -> AppContainer.repository(appContext).toggleFavorite(id, type) }
    )

    private val _items = MutableStateFlow<List<RandomGifVO>>(emptyList())
    val items: StateFlow<List<RandomGifVO>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 1
    private var hasMore = true

    init { refresh() }

    fun refresh() {
        currentPage = 1
        hasMore = true
        _items.value = emptyList()
        _error.value = null
        loadMore()
    }

    fun loadMore() {
        if (_loading.value || !hasMore) return
        _loading.value = true
        viewModelScope.launch {
            loadFavorites(currentPage, PAGE_SIZE)
                .onSuccess { page ->
                    _items.value = _items.value + page
                    hasMore = page.size >= PAGE_SIZE
                    currentPage += 1
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "加载收藏失败" }
            _loading.value = false
        }
    }

    /**
     * 取消收藏。toggle 返回 false 表示已取消，此时即时从列表移除，
     * 避免整页重载导致的闪烁。
     */
    fun removeFavorite(id: Long) {
        viewModelScope.launch {
            toggleFavorite(id, "gif")
                .onSuccess { favorited ->
                    if (!favorited) {
                        _items.value = _items.value.filterNot { it.id == id }
                    }
                }
                .onFailure { _error.value = it.message ?: "取消收藏失败" }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
