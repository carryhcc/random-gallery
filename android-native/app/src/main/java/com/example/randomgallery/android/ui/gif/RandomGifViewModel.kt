package com.example.randomgallery.android.ui.gif

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.BuildConfig
import com.example.randomgallery.android.data.model.RandomGifVO
import com.example.randomgallery.android.data.network.NetworkModule
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.util.ImageUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

class RandomGifViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)

    // 复用全局单例 OkHttpClient
    private val checkClient by lazy {
        NetworkModule.okHttpClient(appContext, BuildConfig.ENABLE_HTTP_LOGGING)
    }

    // 播放模式："single" (单张随机) vs "group" (扑克堆叠套图)
    private val _playMode = MutableStateFlow("single")
    val playMode: StateFlow<String> = _playMode.asStateFlow()

    private val _gifList = MutableStateFlow<List<RandomGifVO>>(emptyList())
    val gifList: StateFlow<List<RandomGifVO>> = _gifList.asStateFlow()

    // 套图卡牌栈列表（当前正在展示的一套扑克堆叠动图）
    private val _currentGroupGifs = MutableStateFlow<List<RandomGifVO>>(emptyList())
    val currentGroupGifs: StateFlow<List<RandomGifVO>> = _currentGroupGifs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var isLoadingMore = false
    private var hasPendingLoad = false
    private val reportedIds = mutableSetOf<Long>() // 防同一session重复上报

    // ── 浏览历史去重：SharedPreferences 持久化 ──
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("gif_prefs", Context.MODE_PRIVATE)
    }
    private val seenIds: MutableSet<Long> by lazy {
        prefs.getStringSet("seen_ids", emptySet())!!.map { it.toLong() }.toMutableSet()
    }

    fun saveSeenIds() {
        prefs.edit().putStringSet("seen_ids", seenIds.map { it.toString() }.toSet()).apply()
    }

    // ── 收藏状态 ──
    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    init {
        preloadBuffer(3)
    }

    fun switchMode(mode: String) {
        if (_playMode.value == mode) return
        _playMode.value = mode
        _gifList.value = emptyList()
        _currentGroupGifs.value = emptyList()
        _error.value = null
        isLoadingMore = false
        hasPendingLoad = false
        if (mode == "group") {
            loadNextGroup()
        } else {
            preloadBuffer(3)
        }
    }

    /** 抽取并加载下一组全新套图，并按顺序提前预热所有海报 */
    fun loadNextGroup() {
        if (isLoadingMore) return
        isLoadingMore = true
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            repository().getRandomGifGroup()
                .onSuccess { groupGifs ->
                    if (groupGifs.isNotEmpty()) {
                        _currentGroupGifs.value = groupGifs
                        _error.value = null
                        // 顺序后台预载该套图所有海报缓存
                        preloadGroupCovers(groupGifs)
                    } else {
                        _error.value = "未找到可用套图动图"
                    }
                }
                .onFailure {
                    _error.value = it.message ?: "加载套图失败"
                }
            _loading.value = false
            isLoadingMore = false
        }
    }

    private fun preloadGroupCovers(gifs: List<RandomGifVO>) {
        val loader = appContext.imageLoader
        gifs.forEach { gif ->
            gif.mediaUrl?.let { url ->
                val displayUrl = ImageUrlResolver.displayUrl(url)
                if (!displayUrl.isNullOrBlank()) {
                    val request = ImageRequest.Builder(appContext)
                        .data(displayUrl)
                        .build()
                    loader.enqueue(request)
                }
            }
        }
    }

    /**
     * 从后端单次拉取一张有效动图
     */
    private suspend fun fetchOneGif(): Boolean {
        var attempts = 0
        var loaded = false
        val excludeParam = if (seenIds.isNotEmpty())
            seenIds.toList().takeLast(500).joinToString(",") else null

        while (attempts < MAX_ATTEMPTS && !loaded) {
            attempts++
            repository().getRandomGif(excludeParam)
                .onSuccess { gif ->
                    val url = gif.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
                    if (url != null && isUrlAlive(url)) {
                        val current = _gifList.value
                        if (current.none { it.mediaUrl == gif.mediaUrl } && current.size < MAX_GIFS) {
                            _gifList.value = current + gif
                        }
                        loaded = true
                        _error.value = null
                        gif.id?.let {
                            seenIds.add(it)
                            saveSeenIds()
                        }
                    } else {
                        gif.id?.let { reportDead(it) }
                    }
                }
                .onFailure {
                    if (_gifList.value.isEmpty()) _error.value = it.message ?: "加载失败"
                    loaded = true
                }
        }
        return loaded
    }

    /**
     * 预热填充缓冲队列，保障快速切换与失效秒切体验
     */
    fun preloadBuffer(targetCount: Int = 3) {
        if (_playMode.value == "group") return
        if (isLoadingMore) {
            hasPendingLoad = true
            return
        }
        isLoadingMore = true
        val isFirst = _gifList.value.isEmpty()
        if (isFirst) _loading.value = true

        viewModelScope.launch {
            try {
                var loadedSoFar = _gifList.value.size
                while (loadedSoFar < targetCount && loadedSoFar < MAX_GIFS) {
                    val ok = fetchOneGif()
                    if (!ok) break
                    loadedSoFar = _gifList.value.size
                }
                while (hasPendingLoad) {
                    hasPendingLoad = false
                    if (_gifList.value.size < MAX_GIFS) {
                        fetchOneGif()
                    }
                }
            } finally {
                if (isFirst) _loading.value = false
                isLoadingMore = false
            }
        }
    }

    fun loadNext() {
        if (_playMode.value == "group") {
            loadNextGroup()
            return
        }

        if (isLoadingMore) {
            hasPendingLoad = true
            return
        }
        isLoadingMore = true
        val isFirst = _gifList.value.isEmpty()
        if (isFirst) _loading.value = true

        viewModelScope.launch {
            try {
                fetchOneGif()
                while (hasPendingLoad) {
                    hasPendingLoad = false
                    if (_gifList.value.size < MAX_GIFS) {
                        fetchOneGif()
                    }
                }
            } finally {
                if (isFirst) _loading.value = false
                isLoadingMore = false
            }
        }
    }

    /**
     * 上报死链到后端（防重复上报，异步非阻塞）
     */
    fun reportDead(id: Long) {
        if (reportedIds.contains(id)) return
        reportedIds.add(id)
        viewModelScope.launch {
            try { repository().reportDead(id) } catch (_: Exception) {}
        }
    }

    /**
     * 查询指定动图的收藏状态
     */
    fun checkFavorite(id: Long) {
        viewModelScope.launch {
            try {
                val result = repository().checkFavorite(id)
                result.onSuccess { _isFavorited.value = it }
            } catch (_: Exception) {}
        }
    }

    /**
     * 切换指定动图的收藏状态。
     *
     * 目标 ID 必须由 UI 传入：展示中的卡片由 activeIndex 决定，而 activeIndex 会随
     * 上下滑动回退，用列表末尾项 / 首项兜底会收藏到并非当前展示的那张图。
     */
    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            try {
                val result = repository().toggleFavorite(id)
                result.onSuccess { _isFavorited.value = it }
            } catch (_: Exception) {}
        }
    }

    private suspend fun isUrlAlive(url: String): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val req = Request.Builder().url(url).head().build()
            val call = checkClient.newCall(req)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 网络抖动或 CDN 不支持 HEAD：乐观认为可用，最终交由 Coil 判定
                    if (continuation.isActive) continuation.resume(true)
                }
                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    response.close()
                    // 仅 404/410 视为确实失效；403/405 等（CDN 拒绝 HEAD）按可用处理
                    val alive = response.isSuccessful || (code != 404 && code != 410)
                    if (continuation.isActive) continuation.resume(alive)
                }
            })
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MAX_GIFS = 120
    }
}
