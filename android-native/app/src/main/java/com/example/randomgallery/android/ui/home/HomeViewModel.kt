package com.example.randomgallery.android.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PicCount
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)
    private val prefs by lazy { AppPrefs(appContext) }

    private val _envInfo = MutableStateFlow<UiState<PicCount>>(UiState.Loading)
    val envInfo: StateFlow<UiState<PicCount>> = _envInfo.asStateFlow()

    private val _privacy = MutableStateFlow(true)
    val privacy: StateFlow<Boolean> = _privacy.asStateFlow()

    private val _localEnv = MutableStateFlow("")
    val localEnv: StateFlow<String> = _localEnv.asStateFlow()

    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _urlList = MutableStateFlow<List<String>>(emptyList())
    val urlList: StateFlow<List<String>> = _urlList.asStateFlow()

    // 一次性 UI 消息（不会在页面返回时重播）
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _randomGroupEvents = Channel<Result<GroupVO>>(Channel.BUFFERED)
    val randomGroupEvents: Flow<Result<GroupVO>> = _randomGroupEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository().baseUrlFlow.collectLatest { _baseUrl.value = it }
        }
        viewModelScope.launch {
            repository().envFlow.collectLatest { _localEnv.value = it }
        }
        viewModelScope.launch {
            repository().privacyFlow.collectLatest { _privacy.value = it }
        }
        viewModelScope.launch {
            repository().urlListFlow.collectLatest { _urlList.value = it }
        }
    }

    fun loadEnvInfo() {
        viewModelScope.launch {
            val result = repository().getCurrentEnvInfo()
            _envInfo.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "加载失败") }
            )
            if (result.isFailure) {
                val url = AppContainer.currentBaseUrl()
                val msg = result.exceptionOrNull()?.message ?: "未知错误"
                _messages.trySend("连接失败 ($url): $msg")
            }
        }
    }

    fun loadPrivacy() {
        viewModelScope.launch {
            repository().getPrivacyMode().onSuccess { _privacy.value = it }
        }
    }

    fun setPrivacy(enabled: Boolean) {
        viewModelScope.launch {
            repository().setPrivacyMode(enabled)
                .onSuccess {
                    _privacy.value = it
                    _messages.trySend(if (it) "隐私模式已开启" else "隐私模式已关闭")
                }
                .onFailure {
                    _privacy.value = !enabled
                    _messages.trySend("失败：${it.message ?: "未知错误"}")
                }
        }
    }

    fun switchEnv(env: String) {
        viewModelScope.launch {
            val result = repository().switchEnv(env)
            _messages.trySend(
                if (result.isSuccess) "已切换到 $env 环境"
                else "切换失败：${result.exceptionOrNull()?.message ?: "未知错误"}"
            )
            loadEnvInfo()
        }
    }

    fun randomGroup() {
        viewModelScope.launch {
            val result = repository().getRandomGroupInfo()
            _randomGroupEvents.trySend(result)
            result.onFailure { _messages.trySend("获取失败：${it.message}") }
        }
    }

    /** 切换到已有 URL（直接应用，不需要验证） */
    fun selectBaseUrl(url: String) {
        viewModelScope.launch {
            AppContainer.updateBaseUrl(appContext, url)
            _baseUrl.value = AppContainer.currentBaseUrl()
            _messages.trySend("已切换到 $url")
            loadEnvInfo()
        }
    }

    /** 添加新 URL 到列表并立即切换 */
    fun addAndSelectUrl(rawUrl: String) {
        val sanitized = BaseUrlConfig.sanitize(rawUrl)
        if (sanitized == null) {
            _messages.trySend("请输入完整的 http:// 或 https:// 地址")
            return
        }
        viewModelScope.launch {
            val current = prefs.getUrlList().toMutableList()
            if (!current.contains(sanitized)) {
                current.add(0, sanitized)
                prefs.saveUrlList(current)
            }
            AppContainer.updateBaseUrl(appContext, sanitized)
            _baseUrl.value = AppContainer.currentBaseUrl()
            _messages.trySend("服务地址已更新，正在连接...")
            loadEnvInfo()
        }
    }

    /** 从列表中删除一个 URL */
    fun removeUrl(url: String) {
        viewModelScope.launch {
            val current = prefs.getUrlList().toMutableList()
            current.remove(url)
            prefs.saveUrlList(current)
        }
    }

    // 兼容旧调用
    fun saveBaseUrl(rawUrl: String) = addAndSelectUrl(rawUrl)
}
