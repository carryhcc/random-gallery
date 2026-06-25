package com.example.randomgallery.android.ui.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.RandomGifVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.util.ImageUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RandomGifViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _gifList = MutableLiveData<List<RandomGifVO>>(emptyList())
    val gifList: LiveData<List<RandomGifVO>> = _gifList

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var isLoadingMore = false

    // 轻量 OkHttp 客户端，仅用于 HEAD 校验，超时短
    private val checkClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    init {
        loadNext()
        loadNext() // 初始预加载两个
    }

    fun loadNext() {
        if (isLoadingMore) return
        isLoadingMore = true
        val isFirst = _gifList.value.isNullOrEmpty()
        if (isFirst) _loading.value = true
        viewModelScope.launch {
            var attempts = 0
            var loaded = false
            while (attempts < 5 && !loaded) {
                attempts++
                repository.getRandomGif()
                    .onSuccess { gif ->
                        val url = gif.mediaUrl?.let { ImageUrlResolver.rawUrl(it) }
                        if (url != null && isUrlAlive(url)) {
                            val current = _gifList.value ?: emptyList()
                            if (current.none { it.mediaUrl == gif.mediaUrl }) {
                                _gifList.value = current + gif
                                loaded = true
                                _error.value = null
                            } else {
                                loaded = true // 重复也算结束，避免死循环
                            }
                        }
                        // url 失效则继续循环尝试下一个
                    }
                    .onFailure {
                        if (isFirst) _error.value = it.message ?: "加载失败"
                        loaded = true // 网络错误就停止重试
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

    override fun onCleared() {
        super.onCleared()
        checkClient.dispatcher.executorService.shutdown()
    }
}
