package com.example.randomgallery.android.testing

import com.example.randomgallery.android.data.local.CachedPayload
import com.example.randomgallery.android.data.local.CachedPayloadDao
import com.example.randomgallery.android.data.local.AppSettings
import com.example.randomgallery.android.data.model.ApiResponse
import com.example.randomgallery.android.data.model.AuthorVO
import com.example.randomgallery.android.data.model.CookieValidateQry
import com.example.randomgallery.android.data.model.DownLoadQry
import com.example.randomgallery.android.data.model.DownloadTaskStatsVO
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PageResult
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.data.model.TagVO
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.data.model.XhsWorkPageVO
import com.example.randomgallery.android.data.model.XhsWorkDetailVO
import com.example.randomgallery.android.data.network.ApiService
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.di.RepositoryProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 内存版 [AppSettings]，替代 DataStore 支撑的 AppPrefs（后者必须有 Android Context）。
 */
class FakePreferences : AppSettings {
    private val env = MutableStateFlow("dev")
    private val privacy = MutableStateFlow(true)
    private val viewMode = MutableStateFlow("single")
    private val baseUrl = MutableStateFlow<String?>(null)
    private val autoRead = MutableStateFlow(false)
    private val urlList = MutableStateFlow<List<String>>(emptyList())
    private val xhsCookie = MutableStateFlow("")
    private val darkMode = MutableStateFlow("system")
    private val spaceMode = MutableStateFlow("explore")
    private val proxyEnabled = MutableStateFlow(false)
    private val proxyType = MutableStateFlow("HTTP")
    private val proxyHost = MutableStateFlow("127.0.0.1")
    private val proxyPort = MutableStateFlow(7890)

    override val envFlow: Flow<String> = env
    override val privacyFlow: Flow<Boolean> = privacy
    override val viewModeFlow: Flow<String> = viewMode
    override val baseUrlFlow: Flow<String?> = baseUrl
    override val autoReadClipboardFlow: Flow<Boolean> = autoRead
    override val urlListFlow: Flow<List<String>> = urlList
    override val xhsCookieFlow: Flow<String> = xhsCookie
    override val darkModeFlow: Flow<String> = darkMode
    override val spaceModeFlow: Flow<String> = spaceMode
    override val proxyEnabledFlow: Flow<Boolean> = proxyEnabled
    override val proxyTypeFlow: Flow<String> = proxyType
    override val proxyHostFlow: Flow<String> = proxyHost
    override val proxyPortFlow: Flow<Int> = proxyPort

    override suspend fun saveEnv(env: String) { this.env.value = env }
    override suspend fun savePrivacy(enabled: Boolean) { privacy.value = enabled }
    override suspend fun saveViewMode(mode: String) { viewMode.value = mode }
    override suspend fun saveAutoReadClipboard(enabled: Boolean) { autoRead.value = enabled }
    override suspend fun saveXhsCookie(cookie: String) { xhsCookie.value = cookie.trim() }
    override suspend fun saveDarkMode(mode: String) { darkMode.value = mode }
    override suspend fun saveSpaceMode(mode: String) { spaceMode.value = mode }

    override suspend fun saveProxyConfig(enabled: Boolean, type: String, host: String, port: Int) {
        proxyEnabled.value = enabled
        proxyType.value = type
        proxyHost.value = host
        proxyPort.value = port
    }

    override suspend fun getBaseUrl(): String? = baseUrl.value
    override suspend fun saveBaseUrl(baseUrl: String?) { this.baseUrl.value = baseUrl }
    override suspend fun getUrlList(): List<String> = urlList.value
    override suspend fun saveUrlList(urls: List<String>) { urlList.value = urls }
}

/** 内存版缓存 DAO。 */
class FakeCacheDao : CachedPayloadDao {
    private val store = mutableMapOf<String, CachedPayload>()
    val writes = mutableListOf<String>()

    override suspend fun upsert(item: CachedPayload) {
        store[item.key] = item
        writes += item.key
    }

    override suspend fun findByKey(key: String): CachedPayload? = store[key]

    override suspend fun clearAll() = store.clear()
}

/**
 * 只桩化测试实际用到的方法，其余一律抛异常。
 *
 * 手写完整实现会掩盖遗漏：ApiService 有 28 个方法，某个漏桩的方法默认返回 null，
 * 测试可能"通过"却什么也没验证。抛异常则会在第一次意外调用时立刻暴露。
 */
open class StubApiService : ApiService {

    /** 每次 `getXhsWorkList` 的入参，便于断言分页/筛选行为。 */
    val requestedPages = mutableListOf<Int>()
    val requestedSeeds = mutableListOf<Int?>()

    /** 桩：作品列表。默认返回空页。 */
    var onGetWorkList: (suspend (page: Int, authorId: String?, tagId: Long?, keyword: String?, seed: Int?) -> ApiResponse<XhsWorkPageVO>)? = null

    override suspend fun getXhsWorkList(
        page: Int,
        size: Int,
        authorId: String?,
        tagId: Long?,
        keyword: String?,
        seed: Int?,
    ): ApiResponse<XhsWorkPageVO> {
        requestedPages += page
        requestedSeeds += seed
        return onGetWorkList?.invoke(page, authorId, tagId, keyword, seed)
            ?: ApiResponse(code = 200, data = XhsWorkPageVO(works = emptyList(), hasMore = false))
    }

    override suspend fun getAuthors(): ApiResponse<List<AuthorVO>> = ApiResponse(code = 200, data = emptyList())
    override suspend fun getTags(limit: Int): ApiResponse<List<TagVO>> = ApiResponse(code = 200, data = emptyList())

    /** 桩：提交下载任务。默认成功。 */
    var onAddDownloadTask: (suspend (url: String) -> ApiResponse<String>)? = null
    val submittedUrls = mutableListOf<String>()

    /** 每次 `addDownloadTask` 携带的 cookie（null 表示未带）。 */
    val submittedCookies = mutableListOf<String?>()

    override suspend fun addDownloadTask(qry: DownLoadQry): ApiResponse<String> {
        submittedUrls += qry.url
        submittedCookies += qry.cookie
        return onAddDownloadTask?.invoke(qry.url) ?: ApiResponse(code = 200, message = "任务已添加")
    }

    /** 桩：下载历史。默认空页 —— 下载管理页在 init 与轮询里都会拉它，默认即失败会淹没测试。 */
    var onGetDownloadHistory: (suspend (page: Int, size: Int, status: Int?) -> ApiResponse<PageResult<XhsDownloadTaskVO>>)? = null

    override suspend fun getDownloadHistory(page: Int, size: Int, status: Int?): ApiResponse<PageResult<XhsDownloadTaskVO>> =
        onGetDownloadHistory?.invoke(page, size, status)
            ?: ApiResponse(code = 200, data = PageResult(list = emptyList(), pages = 1))

    override suspend fun getDownloadStats(): ApiResponse<DownloadTaskStatsVO> =
        ApiResponse(code = 200, data = DownloadTaskStatsVO())

    /** 桩：校验 XHS cookie。未设 hook 即抛异常，避免静默通过。 */
    var onValidateCookie: (suspend (String) -> ApiResponse<Boolean>)? = null

    override suspend fun validateCookie(qry: CookieValidateQry): ApiResponse<Boolean> =
        onValidateCookie?.invoke(qry.cookie) ?: unimplemented("validateCookie")

    // ── 以下未被测试桩化：被调用即失败，避免静默通过 ──
    override suspend fun getRandomPic(): ApiResponse<PicVO> = unimplemented("getRandomPic")
    override suspend fun getPicList(groupId: Long?, pageIndex: Int, pageSize: Int) = unimplemented("getPicList")
    override suspend fun getRandomGroupInfo(groupId: Long?) = unimplemented("getRandomGroupInfo")
    override suspend fun getGroupList(groupName: String?, pageIndex: Int, pageSize: Int): ApiResponse<PageResult<GroupVO>> = unimplemented("getGroupList")
    override suspend fun loadMoreGroups(page: Int, refresh: Boolean) = unimplemented("loadMoreGroups")
    override suspend fun deleteDownloadTask(id: Long) = unimplemented("deleteDownloadTask")
    override suspend fun retryDownloadTask(id: Long) = unimplemented("retryDownloadTask")
    override suspend fun getXhsWorkDetail(workId: String): ApiResponse<XhsWorkDetailVO> = unimplemented("getXhsWorkDetail")
    override suspend fun deleteWork(workId: String) = unimplemented("deleteWork")
    override suspend fun deleteMedia(id: Long) = unimplemented("deleteMedia")
    override suspend fun getRandomGif(exclude: String?) = unimplemented("getRandomGif")
    override suspend fun getRandomGifGroup() = unimplemented("getRandomGifGroup")
    override suspend fun reportDead(id: Long) = unimplemented("reportDead")
    override suspend fun toggleFavorite(id: Long, type: String) = unimplemented("toggleFavorite")
    override suspend fun checkFavorite(id: Long, type: String) = unimplemented("checkFavorite")
    override suspend fun getFavorites(page: Int, size: Int) = unimplemented("getFavorites")
    override suspend fun getPrivacyMode() = unimplemented("getPrivacyMode")
    override suspend fun setPrivacyMode(enabled: Boolean) = unimplemented("setPrivacyMode")
    override suspend fun getCurrentEnv() = unimplemented("getCurrentEnv")
    override suspend fun getCurrentEnvInfo() = unimplemented("getCurrentEnvInfo")
    override suspend fun switchEnv(env: String) = unimplemented("switchEnv")

    private fun unimplemented(name: String): Nothing =
        throw UnsupportedOperationException("ApiService.$name 未在测试中桩化")
}

/**
 * 真实 [GalleryRepository] + 假 ApiService / 假 DAO / 内存偏好。
 *
 * 刻意不为仓库再抽一层接口：那样单测只覆盖到 wrapper，验证不到仓库自身的
 * 分页截断、离线回退、TTL 判定这些真正会出错的逻辑。
 */
class TestRepository(
    onGetWorkList: (suspend (Int, String?, Long?, String?, Int?) -> ApiResponse<XhsWorkPageVO>)? = null,
) {
    val api = StubApiService().apply { this.onGetWorkList = onGetWorkList }
    val prefs = FakePreferences()
    val cacheDao = FakeCacheDao()

    val requestedPages: List<Int> get() = api.requestedPages
    val requestedSeeds: List<Int?> get() = api.requestedSeeds
    val submittedUrls: List<String> get() = api.submittedUrls

    val repository: GalleryRepository = GalleryRepository(
        api = api,
        cacheDao = cacheDao,
        prefs = prefs,
    )

    /** 供 ViewModel 构造器使用的取用能力。 */
    val provider: RepositoryProvider = RepositoryProvider { repository }
}

/** 构造一页作品。 */
fun worksPage(ids: List<String>, hasMore: Boolean = true) = XhsWorkPageVO(
    works = ids.map { id ->
        XhsWorkListVO(
            id = id.removePrefix("w").toLongOrNull(),
            workId = id,
            workTitle = "作品 $id",
            coverImageUrl = "https://cdn.example.com/$id.jpg",
        )
    },
    hasMore = hasMore,
)
