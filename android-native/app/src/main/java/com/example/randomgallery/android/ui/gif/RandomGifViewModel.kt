package com.example.randomgallery.android.ui.gif

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.withContext
import okhttp3.Request

class RandomGifViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)

    // 复用全局单例 OkHttpClient
    private val checkClient by lazy {
        NetworkModule.okHttpClient(appContext, BuildConfig.ENABLE_HTTP_LOGGING)
    }

    // 播放模式："single" (单张随机) vs "group" (套图随机)
    private val _playMode = MutableStateFlow("single")
    val playMode: StateFlow<String> = _playMode.asStateFlow()

    private val _gifList = MutableStateFlow<List<RandomGifVO>>(emptyList())
    val gifList: StateFlow<List<RandomGifVO>> = _gifList.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var isLoadingMore = false

    init {
        loadNext()
    }

    fun switchMode(mode: String) {
        if (_playMode.value == mode) return
        _playMode.value = mode
        _gifList.value = emptyList()
        _error.value = null
        isLoadingMore = false
        loadNext()
    }

    fun loadNext() {
        if (isLoadingMore) return
        isLoadingMore = true
        val isFirst = _gifList.value.isEmpty()
        if (isFirst) _loading.value = true

        viewModelScope.launch {
            if (_playMode.value == "group") {
                // 套图模式：随机拉取同一作品下的一整组动图
                repository().getRandomGifGroup()
                    .onSuccess { groupGifs ->
                        if (groupGifs.isNotEmpty()) {
                            val current = _gifList.value
                            val filtered = groupGifs.filter { g -> current.none { it.id == g.id } }
                            _gifList.value = (current + filtered).take(MAX_GIFS)
                            _error.value = null
                        } else {
                            if (isFirst) _error.value = "未找到可用套图动图"
                        }
                    }
                    .onFailure {
                        if (isFirst) _error.value = it.message ?: "加载失败"
                    }
            } else {
                // 单张随机模式
                var attempts = 0
                var loaded = false
                while (attempts < MAX_ATTEMPTS && !loaded) {
                    attempts++
                    repository().getRandomGif()
                        .onSuccess { gif ->
                            val url = gif.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
                            if (url != null && isUrlAlive(url)) {
                                val current = _gifList.value
                                if (current.none { it.mediaUrl == gif.mediaUrl } && current.size < MAX_GIFS) {
                                    _gifList.value = current + gif
                                }
                                loaded = true
                                _error.value = null
                            }
                        }
                        .onFailure {
                            if (isFirst) _error.value = it.message ?: "加载失败"
                            loaded = true
                        }
                }
            }
            if (isFirst) _loading.value = false
            isLoadingMore = false
        }
    }

    private suspend fun isUrlAlive(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).head().build()
            val resp = checkClient.newCall(req).execute()
            val code = resp.code
            resp.close()
            code in 200..399
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MAX_GIFS = 120
    }
}
