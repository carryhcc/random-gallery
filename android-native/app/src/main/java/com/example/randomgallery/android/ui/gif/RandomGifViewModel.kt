package com.example.randomgallery.android.ui.gif

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.randomgallery.android.data.model.RandomGifVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.di.BaseUrlManager
import com.example.randomgallery.android.util.ImageUrlResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class RandomGifViewModel @Inject constructor(
    // @param: 明确把限定符落在构造参数上：Kotlin 2.2 默认还会把它复制到字段，
    // 那会让 Dagger 同时看到参数与字段两个注入点，这里显式收敛回参数。
    @param:ApplicationContext private val appContext: Context,
    private val baseUrlManager: BaseUrlManager,
    private val checkClient: OkHttpClient,
) : ViewModel() {

    private fun repository(): GalleryRepository = baseUrlManager.repository()

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

    // 套图预热任务：换组时取消上一轮，避免旧组的海报转储与新组抢占解码线程
    private var preloadJob: Job? = null

    // ── 浏览历史去重：SharedPreferences 持久化 ──
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("gif_prefs", Context.MODE_PRIVATE)
    }
    // 浏览历史去重集合：有界。
    //
    // 两个问题原先都存在：(1) getStringSet(...)!! 一旦存储内容被外部改坏或版本升级
    // 迁移出非数字串，toLong() 会抛 NumberFormatException 直接崩溃；(2) 集合只增不减，
    // 而每次 fetchOneGif 成功都要把整个集合重新序列化写盘，浏览越久写得越大。
    // 去重的实际意义只在「近期不重复」，因此保留最近 SEEN_IDS_LIMIT 条即可。
    private val seenIds: MutableSet<Long> by lazy {
        prefs.getStringSet(KEY_SEEN_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toMutableSet()
    }

    fun saveSeenIds() {
        // toMutableSet() 返回 LinkedHashSet，保插入顺序，因此尾部就是最近浏览的
        if (seenIds.size > SEEN_IDS_LIMIT) {
            val keep = seenIds.toList().takeLast(SEEN_IDS_LIMIT)
            seenIds.clear()
            seenIds.addAll(keep)
        }
        prefs.edit().putStringSet(KEY_SEEN_IDS, seenIds.map { it.toString() }.toSet()).apply()
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
        // 只预热用户马上会看到的前几张：整组一次性并发入队会瞬间占满 Coil 的解码线程池，
        // 反而把当前这张卡的解码挤到后面，得不偿失。
        val urls = gifs.asSequence()
            .mapNotNull { it.mediaUrl }
            .map { ImageUrlResolver.displayUrl(it) }
            .filter { !it.isNullOrBlank() }
            .take(PRELOAD_COVER_LIMIT)
            .toList()

        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            urls.forEach { displayUrl ->
                val request = ImageRequest.Builder(appContext)
                    .data(displayUrl)
                    // 必须显式给尺寸：脱离 composition 的请求没有 ConstraintsSizeResolver 兜底，
                    // 不给尺寸 Coil 会按 Size.ORIGINAL 解码原图，等于把整组大图全量解进内存。
                    .size(PRELOAD_COVER_EDGE, PRELOAD_COVER_EDGE)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    /**
     * 从后端单次拉取一张有效动图
     */
    private suspend fun fetchOneGif(): Boolean {
        var attempts = 0
        var loaded = false
        // 排除列表直接拼进 URL query：500 个雪花 ID 约 10KB，已逼近部分代理/CDN 的
        // 请求行上限（常见 8KB），超了会被 414 掉。取最近 200 条即可满足「近期不重复」。
        val excludeParam = if (seenIds.isNotEmpty())
            seenIds.toList().takeLast(EXCLUDE_IDS_LIMIT).joinToString(",") else null

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

        // 套图海报预热：只做前几张，且按缩略图尺寸解码
        const val PRELOAD_COVER_LIMIT = 3
        const val PRELOAD_COVER_EDGE = 512

        // 浏览去重集合上限（同时约束 SharedPreferences 写入体积）
        const val SEEN_IDS_LIMIT = 500
        const val KEY_SEEN_IDS = "seen_ids"
        // 单次请求携带的排除 ID 上限（约束 URL 长度）
        const val EXCLUDE_IDS_LIMIT = 200
    }
}
