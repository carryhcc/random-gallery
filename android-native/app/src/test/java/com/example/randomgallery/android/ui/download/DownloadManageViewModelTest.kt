package com.example.randomgallery.android.ui.download

import com.example.randomgallery.android.data.model.ApiResponse
import com.example.randomgallery.android.data.model.PageResult
import com.example.randomgallery.android.data.model.XhsDownloadTaskVO
import com.example.randomgallery.android.testing.TestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `DownloadManageViewModel` 纯 JVM 单测。
 *
 * 重点覆盖下载链接的输入清洗：用户从剪贴板/分享面板拿到的常常是整段分享文案
 * （含标题、表情、短链），需要从中挑出第一个 http(s) 地址。
 *
 * 注意提交给后端的是**原始整段文本**（后端负责提取并留存原始 params_url），
 * 因此 [DownloadManageViewModel.extractHttpUrl] 只用于向用户回显解析结果，
 * 断言要分开看这两个通道。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadManageViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: TestRepository = TestRepository(),
        history: (Int, Int, Int?) -> ApiResponse<PageResult<XhsDownloadTaskVO>> = { _, _, _ ->
            ApiResponse(code = 200, data = PageResult(list = emptyList(), pages = 1))
        },
    ): DownloadManageViewModel {
        repo.api.onGetDownloadHistory = { page, size, status -> history(page, size, status) }
        return DownloadManageViewModel(repo.provider)
    }

    // ── 纯函数：链接提取 ────────────────────────────────────────────

    @Test
    fun `plain url is extracted as-is`() {
        assertEquals(
            "https://www.xiaohongshu.com/explore/abc123",
            DownloadManageViewModel.extractHttpUrl("https://www.xiaohongshu.com/explore/abc123"),
        )
    }

    @Test
    fun `url is extracted out of sharing text`() {
        // 实测抓取的分享文案形态：标题 + 短链 + 表情
        assertEquals(
            "http://xhslink.com/a/bCdEf",
            DownloadManageViewModel.extractHttpUrl(
                "超好看的旅行照片 😍 http://xhslink.com/a/bCdEf 复制本条信息打开小红书"
            ),
        )
    }

    @Test
    fun `text without a url yields null`() {
        assertNull(DownloadManageViewModel.extractHttpUrl("这段文字里没有链接"))
        assertNull(DownloadManageViewModel.extractHttpUrl("   "))
        assertNull(DownloadManageViewModel.extractHttpUrl(null))
    }

    // ── 提交路径 ───────────────────────────────────────────────────

    @Test
    fun `submitting a shared snippet strips the url for display but sends the original text`() {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.submit("超好看的旅行照片 😍 http://xhslink.com/a/bCdEf 复制本条信息打开小红书")

        // 回显用提取结果
        assertEquals("http://xhslink.com/a/bCdEf", vm.lastResolvedUrl.value)
        // 提交用原文（后端需要原始 params_url）
        assertEquals(
            listOf("超好看的旅行照片 😍 http://xhslink.com/a/bCdEf 复制本条信息打开小红书"),
            repo.submittedUrls,
        )
    }

    @Test
    fun `input is trimmed before submitting`() {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.submit("   https://example.com/work/1   ")

        assertEquals("https://example.com/work/1", vm.lastResolvedUrl.value)
        assertEquals(listOf("https://example.com/work/1"), repo.submittedUrls)
    }

    @Test
    fun `blank input is rejected and never submitted`() {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.submit("   ")

        assertNull("空白输入不应解析出地址", vm.lastResolvedUrl.value)
        assertEquals("空白输入不应发出请求", emptyList<String>(), repo.submittedUrls)
    }

    @Test
    fun `submit ends with loading cleared`() {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.submit("https://example.com/work/1")

        assertFalse("提交结束后不应仍处于加载态", vm.loading.value)
    }

    @Test
    fun `submit attaches saved cookie to download request`() = runTest {
        val repo = TestRepository()
        repo.prefs.saveXhsCookie("a1=abc; web_session=xyz")
        val vm = viewModel(repo)

        vm.submit("some xhs url")

        assertEquals(
            listOf("a1=abc; web_session=xyz"),
            repo.api.submittedCookies,
        )
    }

    @Test
    fun `submit without saved cookie sends null cookie`() = runTest {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.submit("https://example.com/work/1")

        assertEquals(listOf<String?>(null), repo.api.submittedCookies)
    }

    @Test
    fun `submission failure still clears the loading flag`() {
        val repo = TestRepository()
        repo.api.onAddDownloadTask = { ApiResponse(code = 500, message = "服务异常") }
        val vm = viewModel(repo)

        vm.submit("https://example.com/work/1")

        assertFalse("失败也必须收敛加载态，否则按钮永久禁点", vm.loading.value)
    }

    @Test
    fun `history failure clears the loading flag`() {
        val repo = TestRepository()
        val vm = viewModel(repo) { _, _, _ -> throw RuntimeException("网络不可达") }

        vm.loadHistory(showLoading = true)

        assertFalse("历史加载失败也必须清掉加载态", vm.historyLoading.value)
    }

    // ── Cookie 格式闸门 ────────────────────────────────────────────

    @Test
    fun `cookie format gate accepts plausible and rejects malformed`() {
        assertFalse("null 不可用", DownloadManageViewModel.isCookieFormatPlausible(null))
        assertFalse("空串不可用", DownloadManageViewModel.isCookieFormatPlausible(""))
        assertFalse("纯空格不可用", DownloadManageViewModel.isCookieFormatPlausible("   "))
        assertFalse("过短不可用", DownloadManageViewModel.isCookieFormatPlausible("ab=cd"))
        assertFalse("缺少等号不可用", DownloadManageViewModel.isCookieFormatPlausible("noequalsignbutlongenoughtopasslength"))
        assertFalse("含换行不可用", DownloadManageViewModel.isCookieFormatPlausible("a1=abc123\nweb_session=xyz789"))
        assertTrue(
            "devtools 复制的 cookie 应通过",
            DownloadManageViewModel.isCookieFormatPlausible("a1=abc123; web_session=xyz789"),
        )
    }

    @Test
    fun `saveCookie rejects malformed input`() = runTest {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.saveCookie("short")

        assertEquals("格式非法不得落库", "", repo.prefs.xhsCookieFlow.first())
        val event = vm.cookieEvents.first()
        assertTrue("应发出失败事件", event.isFailure)
        assertEquals("Cookie 格式不正确", event.exceptionOrNull()?.message)
    }

    @Test
    fun `saved cookie surfaces in state and resets status to SAVED`() = runTest {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.saveCookie("a1=abc123; web_session=xyz789")

        assertEquals("a1=abc123; web_session=xyz789", vm.xhsCookie.value)
        assertEquals(CookieStatus.SAVED, vm.cookieStatus.value)
    }

    @Test
    fun `validateCookie transitions to VALID on success`() = runTest {
        val repo = TestRepository()
        repo.api.onValidateCookie = { ApiResponse(code = 200, message = "ok", data = true) }
        val vm = viewModel(repo)
        vm.saveCookie("a1=abc123; web_session=xyz789")
        assertTrue(vm.cookieEvents.first().isSuccess)

        vm.validateCookie()

        assertEquals(CookieStatus.VALID, vm.cookieStatus.value)
        assertTrue(vm.cookieEvents.first().isSuccess)
    }

    @Test
    fun `validateCookie transitions to INVALID on rejected cookie`() = runTest {
        val repo = TestRepository()
        repo.api.onValidateCookie = { ApiResponse(code = 200, message = "expired", data = false) }
        val vm = viewModel(repo)
        vm.saveCookie("a1=abc123; web_session=xyz789")
        assertTrue(vm.cookieEvents.first().isSuccess)

        vm.validateCookie()

        assertEquals(CookieStatus.INVALID, vm.cookieStatus.value)
        assertTrue(vm.cookieEvents.first().isFailure)
    }

    @Test
    fun `validateCookie keeps SAVED and emits error on transport failure`() = runTest {
        val repo = TestRepository()
        repo.api.onValidateCookie = { throw RuntimeException("boom") }
        val vm = viewModel(repo)
        vm.saveCookie("a1=abc123; web_session=xyz789")
        assertTrue(vm.cookieEvents.first().isSuccess)

        vm.validateCookie()

        assertEquals("传输失败不得推断 cookie 失效", CookieStatus.SAVED, vm.cookieStatus.value)
        assertTrue(vm.cookieEvents.first().isFailure)
    }

    @Test
    fun `validateCookie rejects when nothing saved`() = runTest {
        val repo = TestRepository()
        val vm = viewModel(repo)

        vm.validateCookie()

        assertEquals(CookieStatus.EMPTY, vm.cookieStatus.value)
        val event = vm.cookieEvents.first()
        assertTrue(event.isFailure)
        assertEquals("请先保存 Cookie", event.exceptionOrNull()?.message)
    }
    @Test
    fun `old validation does not overwrite an updated cookie`() = runTest {
        val repo = TestRepository()
        val response = kotlinx.coroutines.CompletableDeferred<ApiResponse<Boolean>>()
        repo.api.onValidateCookie = { response.await() }
        val vm = viewModel(repo)
        vm.saveCookie("a1=abc123; web_session=old789")
        vm.cookieEvents.first()
        vm.validateCookie()
        assertEquals(CookieStatus.CHECKING, vm.cookieStatus.value)

        vm.saveCookie("a1=abc123; web_session=new789")
        response.complete(ApiResponse(code = 200, data = true))

        assertEquals("a1=abc123; web_session=new789", vm.xhsCookie.value)
        assertEquals(CookieStatus.SAVED, vm.cookieStatus.value)
        vm.submit("https://www.xiaohongshu.com/explore/abc")
        assertEquals(listOf("a1=abc123; web_session=new789"), repo.api.submittedCookies)
    }

    @Test
    fun `save then validate uses persisted cookie and clear removes it from submission`() = runTest {
        val repo = TestRepository()
        var validated: String? = null
        repo.api.onValidateCookie = {
            validated = it
            ApiResponse(code = 200, data = true)
        }
        val vm = viewModel(repo)
        vm.saveCookie("a1=abc123; web_session=new789", validateAfterSave = true)
        assertEquals("a1=abc123; web_session=new789", validated)
        assertEquals(CookieStatus.VALID, vm.cookieStatus.value)
        assertFalse(vm.cookieSaving.value)

        vm.saveCookie("")
        vm.submit("https://www.xiaohongshu.com/explore/abc")
        assertEquals(CookieStatus.EMPTY, vm.cookieStatus.value)
        assertEquals(listOf<String?>(null), repo.api.submittedCookies)
    }

    @Test
    fun `cookie rejects header injection and incomplete pairs`() {
        assertFalse(DownloadManageViewModel.isCookieFormatPlausible("a1=abc123; web_session=xyz\t789"))
        assertFalse(DownloadManageViewModel.isCookieFormatPlausible("a1=abc123; missing_pair"))
        assertFalse(DownloadManageViewModel.isCookieFormatPlausible("a1=abc123; bad name=value"))
        assertTrue(DownloadManageViewModel.isCookieFormatPlausible("a1=abc123; token=abc=="))
    }

    @Test
    fun `storage failure preserves cookie and reports error`() = runTest {
        val repo = TestRepository()
        repo.prefs.saveXhsCookie("a1=abc123; web_session=old789")
        val prefs = object : com.example.randomgallery.android.data.local.AppSettings by repo.prefs {
            override suspend fun saveXhsCookie(cookie: String) { throw java.io.IOException("disk full") }
        }
        val repository = com.example.randomgallery.android.data.repository.GalleryRepository(repo.api, repo.cacheDao, prefs)
        val vm = DownloadManageViewModel(com.example.randomgallery.android.di.RepositoryProvider { repository })

        vm.saveCookie("a1=abc123; web_session=new789")

        assertFalse(vm.cookieSaving.value)
        assertEquals("a1=abc123; web_session=old789", vm.xhsCookie.value)
        assertEquals(CookieStatus.SAVED, vm.cookieStatus.value)
        assertTrue(vm.cookieEvents.first().isFailure)
    }

    @Test
    fun `submission waits for cookie save to complete`() = runTest {
        val repo = TestRepository()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val prefs = object : com.example.randomgallery.android.data.local.AppSettings by repo.prefs {
            override suspend fun saveXhsCookie(cookie: String) {
                gate.await()
                repo.prefs.saveXhsCookie(cookie)
            }
        }
        val repository = com.example.randomgallery.android.data.repository.GalleryRepository(repo.api, repo.cacheDao, prefs)
        val vm = DownloadManageViewModel(com.example.randomgallery.android.di.RepositoryProvider { repository })
        vm.saveCookie("a1=abc123; web_session=new789")
        assertTrue(vm.cookieSaving.value)
        vm.submit("https://www.xiaohongshu.com/explore/abc")
        assertTrue(repo.submittedUrls.isEmpty())

        gate.complete(Unit)
        runCurrent()
        vm.submit("https://www.xiaohongshu.com/explore/abc")
        assertEquals(listOf("a1=abc123; web_session=new789"), repo.api.submittedCookies)
    }

}
