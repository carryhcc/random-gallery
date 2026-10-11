package com.example.randomgallery.android.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.di.BaseUrlManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
@HiltViewModel
class FavoriteViewModel(
    private val loadFavorites: suspend (page: Int, size: Int) -> Result<List<RandomGifVO>>,
    private val toggleFavorite: suspend (id: Long, type: String) -> Result<Boolean>
) : ViewModel() {

    /**
     * 生产环境构造器：从依赖图取仓库。
     *
     * 主构造器保持为两个 suspend 函数，是为了让单测能在不起 Room / Context / 网络的前提下
     * 直接注入假的取数与切换行为（见 FavoriteViewModelTest）。仓库每次调用现取，
     * 因此换节点后本实例的下一次请求自然指向新地址。
     */
    @Inject
    constructor(baseUrlManager: BaseUrlManager) : this(
        loadFavorites = { page, size -> baseUrlManager.repository().getFavorites(page, size) },
        toggleFavorite = { id, type -> baseUrlManager.repository().toggleFavorite(id, type) }
    )

    private val _items = MutableStateFlow<List<RandomGifVO>>(emptyList())
    val items: StateFlow<List<RandomGifVO>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 1
    private var hasMore = true

    // 正在取消收藏的 id 集合。
    //
    // 后端是 toggle 语义而非 delete：0.5~1s 往返窗口内连点两次心形，第二次会把它
    // 重新收藏回来，而列表因为乐观移除已经看不到这一项了。按 id 去重即可，
    // 不用整体互斥——同时取消两个不同条目是合理操作，不该被挡住。
    private val removalInFlight = mutableSetOf<Long>()

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
     *
     * 同一 id 的重复请求直接丢弃（见 [removalInFlight]）。
     */
    fun removeFavorite(id: Long) {
        if (!removalInFlight.add(id)) return
        viewModelScope.launch {
            try {
                toggleFavorite(id, "gif")
                    .onSuccess { favorited ->
                        if (!favorited) {
                            _items.value = _items.value.filterNot { it.id == id }
                        }
                    }
                    .onFailure { _error.value = it.message ?: "取消收藏失败" }
            } finally {
                removalInFlight.remove(id)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
