package com.example.randomgallery.android.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PicCount
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.SingleLiveEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)
    private val prefs by lazy { AppPrefs(appContext) }

    private val _envInfo = MutableLiveData<Result<PicCount>>()
    val envInfo: LiveData<Result<PicCount>> = _envInfo

    private val _privacy = MutableLiveData<Boolean>(true)
    val privacy: LiveData<Boolean> = _privacy

    private val _localEnv = MutableLiveData<String>()
    val localEnv: LiveData<String> = _localEnv

    private val _baseUrl = MutableLiveData<String>()
    val baseUrl: LiveData<String> = _baseUrl

    private val _urlList = MutableLiveData<List<String>>(emptyList())
    val urlList: LiveData<List<String>> = _urlList

    private val _baseUrlMessage = SingleLiveEvent<Result<String>>()
    val baseUrlMessage: LiveData<Result<String>> = _baseUrlMessage

    private val _randomGroup = SingleLiveEvent<Result<GroupVO>>()
    val randomGroup: LiveData<Result<GroupVO>> = _randomGroup

    private val _privacyMessage = SingleLiveEvent<Result<String>>()
    val privacyMessage: LiveData<Result<String>> = _privacyMessage

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
            _envInfo.value = result
            if (result.isFailure) {
                val url = AppContainer.currentBaseUrl()
                val msg = result.exceptionOrNull()?.message ?: "未知错误"
                _baseUrlMessage.value = Result.failure(Exception("连接失败 ($url): $msg"))
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
                    _privacyMessage.value = Result.success(if (it) "隐私模式已开启" else "隐私模式已关闭")
                }
                .onFailure {
                    _privacy.value = !enabled
                    _privacyMessage.value = Result.failure(it)
                }
        }
    }

    fun switchEnv(env: String) {
        viewModelScope.launch {
            repository().switchEnv(env)
            loadEnvInfo()
        }
    }

    fun randomGroup() {
        viewModelScope.launch {
            _randomGroup.value = repository().getRandomGroupInfo()
        }
    }

    /** 切换到已有 URL（直接应用，不需要验证） */
    fun selectBaseUrl(url: String) {
        viewModelScope.launch {
            AppContainer.updateBaseUrl(appContext, url)
            _baseUrl.value = AppContainer.currentBaseUrl()
            _baseUrlMessage.value = Result.success("已切换到 $url")
            loadEnvInfo()
        }
    }

    /** 添加新 URL 到列表并立即切换 */
    fun addAndSelectUrl(rawUrl: String) {
        val sanitized = BaseUrlConfig.sanitize(rawUrl)
        if (sanitized == null) {
            _baseUrlMessage.value = Result.failure(
                IllegalArgumentException("请输入完整的 http:// 或 https:// 地址")
            )
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
            _baseUrlMessage.value = Result.success("服务地址已更新，正在连接...")
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
