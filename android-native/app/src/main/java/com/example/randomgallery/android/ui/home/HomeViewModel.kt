package com.example.randomgallery.android.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PicCount
import com.example.randomgallery.android.data.model.XhsWorkListVO
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

    // 顶部 Hero 左右轮播展示的精选下载作品列表
    private val _heroWorks = MutableStateFlow<List<XhsWorkListVO>>(emptyList())
    val heroWorks: StateFlow<List<XhsWorkListVO>> = _heroWorks.asStateFlow()

    private val _heroLoading = MutableStateFlow(false)
    val heroLoading: StateFlow<Boolean> = _heroLoading.asStateFlow()

    // 首页直接嵌入的下载列表探索流（瀑布流）
    private val _feedWorks = MutableStateFlow<List<XhsWorkListVO>>(emptyList())
    val feedWorks: StateFlow<List<XhsWorkListVO>> = _feedWorks.asStateFlow()

    private val _feedLoading = MutableStateFlow(false)
    val feedLoading: StateFlow<Boolean> = _feedLoading.asStateFlow()

    private var feedPage = 1
    private var hasMoreFeed = true

    private val _privacy = MutableStateFlow(true)
    val privacy: StateFlow<Boolean> = _privacy.asStateFlow()

    private val _darkMode = MutableStateFlow("system")
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()

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

    private val _proxyEnabled = MutableStateFlow(false)
    val proxyEnabled: StateFlow<Boolean> = _proxyEnabled.asStateFlow()

    private val _proxyType = MutableStateFlow("HTTP")
    val proxyType: StateFlow<String> = _proxyType.asStateFlow()

    private val _proxyHost = MutableStateFlow("127.0.0.1")
    val proxyHost: StateFlow<String> = _proxyHost.asStateFlow()

    private val _proxyPort = MutableStateFlow(7890)
    val proxyPort: StateFlow<Int> = _proxyPort.asStateFlow()

    private val _spaceMode = MutableStateFlow("explore")
    val spaceMode: StateFlow<String> = _spaceMode.asStateFlow()

    // ── 环境图库模式专有数据 ─────────────────────────────────────
    private val _galleryGroups = MutableStateFlow<List<GroupVO>>(emptyList())
    val galleryGroups: StateFlow<List<GroupVO>> = _galleryGroups.asStateFlow()

    private val _galleryLoading = MutableStateFlow(false)
    val galleryLoading: StateFlow<Boolean> = _galleryLoading.asStateFlow()

    private var galleryPage = 0
    private var hasMoreGallery = true

    init {
        viewModelScope.launch {
            prefs.proxyEnabledFlow.collectLatest { _proxyEnabled.value = it }
        }
        viewModelScope.launch {
            prefs.proxyTypeFlow.collectLatest { _proxyType.value = it }
        }
        viewModelScope.launch {
            prefs.proxyHostFlow.collectLatest { _proxyHost.value = it }
        }
        viewModelScope.launch {
            prefs.proxyPortFlow.collectLatest { _proxyPort.value = it }
        }
        viewModelScope.launch {
            prefs.spaceModeFlow.collectLatest { _spaceMode.value = it }
        }
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
            prefs.darkModeFlow.collectLatest { _darkMode.value = it }
        }
        viewModelScope.launch {
            repository().urlListFlow.collectLatest { _urlList.value = it }
        }
        // 只加载一次（仓库层有 TTL），避免每次切回首页都重复请求
        loadEnvInfo()
        loadPrivacy()
        loadHeroWorks(force = true)
        loadFeed(refresh = true)
        loadGalleryGroups(refresh = true)
    }

    fun setSpaceMode(mode: String) {
        _spaceMode.value = mode
        viewModelScope.launch {
            prefs.saveSpaceMode(mode)
        }
    }

    fun loadGalleryGroups(refresh: Boolean = false) {
        if (_galleryLoading.value && !refresh) return
        _galleryLoading.value = true
        viewModelScope.launch {
            if (refresh) {
                galleryPage = 0
                hasMoreGallery = true
            }
            repository().loadMoreGroups(page = galleryPage, refresh = refresh)
                .onSuccess { res ->
                    val merged = if (refresh) res.images else _galleryGroups.value + res.images
                    _galleryGroups.value = merged.take(MAX_FEED_WORKS)
                    hasMoreGallery = res.hasMore && merged.size < MAX_FEED_WORKS
                    galleryPage += 1
                }
                .onFailure {
                    // 静默处理
                }
            _galleryLoading.value = false
        }
    }

    fun refreshGallery() {
        galleryPage = 0
        hasMoreGallery = true
        loadEnvInfo(force = true)
        loadGalleryGroups(refresh = true)
    }

    private var feedSeed: Int? = null

    fun loadHeroWorks(force: Boolean = false) {
        if (_heroLoading.value && !force) return
        viewModelScope.launch {
            _heroLoading.value = true
            // 每次刷新/抽选时传入随机 seed，使 Hero 轮播图每次下拉刷新都能换一批精选作品
            val randomSeed = (1..9999).random()
            val res = repository().getWorkList(
                page = 1,
                size = 8,
                authorId = null,
                tagId = null,
                keyword = null,
                seed = randomSeed
            )
            res.onSuccess { page ->
                if (page.works.isNotEmpty()) {
                    _heroWorks.value = page.works
                }
            }.onFailure {
                // 静默重试或保持现有数据
            }
            _heroLoading.value = false
        }
    }

    fun refreshFeed() {
        feedPage = 1
        hasMoreFeed = true
        feedSeed = (1..9999).random() // 下拉刷新时生成全新的随机种子，确保探索列表乱序换新一批
        loadHeroWorks(force = true)
        loadEnvInfo(force = true)
        loadFeed(refresh = true)
    }

    fun loadFeed(refresh: Boolean = false) {
        if (_feedLoading.value && !refresh) return
        _feedLoading.value = true
        viewModelScope.launch {
            if (refresh) {
                feedPage = 1
                hasMoreFeed = true
            }
            repository().getWorkList(
                page = feedPage,
                size = 10,
                authorId = null,
                tagId = null,
                keyword = null,
                seed = feedSeed
            ).onSuccess { res ->
                val merged = if (refresh) res.works else _feedWorks.value + res.works
                _feedWorks.value = merged.take(MAX_FEED_WORKS)
                hasMoreFeed = res.hasMore && merged.size < MAX_FEED_WORKS
                feedPage += 1
            }.onFailure {
                // 静默处理或保留已有数据
            }
            _feedLoading.value = false
        }
    }

    private companion object {
        const val MAX_FEED_WORKS = 120
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            prefs.saveDarkMode(mode)
        }
    }

    fun loadEnvInfo(force: Boolean = false) {
        viewModelScope.launch {
            val result = repository().getCurrentEnvInfo(force)
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

    fun saveProxyConfig(enabled: Boolean, type: String, host: String, port: Int) {
        viewModelScope.launch {
            prefs.saveProxyConfig(enabled, type, host, port)
            // 重建底层网络客户端与连接池
            AppContainer.clearRepository()
            _messages.trySend(if (enabled) "已启用自定义代理 $type://$host:$port" else "已关闭自定义网络代理")
        }
    }

    fun loadPrivacy(force: Boolean = false) {
        viewModelScope.launch {
            repository().getPrivacyMode(force).onSuccess { _privacy.value = it }
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
            loadEnvInfo(force = true)
            // 切换环境后同时重新刷新本地图库的套图分组目录数据
            refreshGallery()
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
            loadEnvInfo(force = true)
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
            loadEnvInfo(force = true)
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
