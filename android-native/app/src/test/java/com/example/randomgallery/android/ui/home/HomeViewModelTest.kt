package com.example.randomgallery.android.ui.home

import com.example.randomgallery.android.data.model.ApiResponse
import com.example.randomgallery.android.data.network.ProxySettings
import com.example.randomgallery.android.di.NodeManager
import com.example.randomgallery.android.di.RepositoryProvider
import com.example.randomgallery.android.testing.TestRepository
import com.example.randomgallery.android.testing.worksPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `HomeViewModel` 纯 JVM 单测。
 *
 * 覆盖探索流的截断上界与种子行为 —— 这两处都是"改完不容易看出来"的：
 * 上界失效会让长会话内存无界增长；种子不换则下拉刷新拿到的还是同一批顺序。
 *
 * 注意 `init` 自身就会加载一次探索流（`loadFeed(refresh = true)`），因此断言都是
 * 相对这个已加载的首页而言的增量，而不是"从未加载开始"。
 *
 * 首页需要改服务地址，因此这里用 [NodeManager] 而非窄的 RepositoryProvider。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val pageSize = 10

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeNodeManager(private val repoProvider: RepositoryProvider) : NodeManager {
        var current = "http://test.invalid/"
        val updatedTo = mutableListOf<String>()
        var invalidated = 0

        override fun repository() = repoProvider.repository()
        override fun currentBaseUrl() = current
        override suspend fun updateBaseUrl(rawUrl: String) {
            updatedTo += rawUrl
            current = rawUrl
        }
        override fun invalidateRepository() { invalidated++ }
    }

    private class RecordingProxySettings : ProxySettings {
        var refreshes = 0
        override suspend fun refresh() { refreshes++ }
    }

    /** 每次调用返回 [pageSize] 条新作品，id 递增，`hasMore` 恒为 true。 */
    private fun pagingRepo(): TestRepository {
        var n = 0
        return TestRepository { _, _, _, _, _ ->
            ApiResponse(code = 200, data = worksPage((1..pageSize).map { "w${n++}" }, hasMore = true))
        }
    }

    private fun viewModel(repo: TestRepository): Triple<HomeViewModel, FakeNodeManager, RecordingProxySettings> {
        val nodes = FakeNodeManager(repo.provider)
        val proxy = RecordingProxySettings()
        val vm = HomeViewModel(
            nodes = nodes,
            prefs = repo.prefs,
            proxySettings = proxy,
        )
        return Triple(vm, nodes, proxy)
    }

    @Test
    fun `feed loads on init and appends on the next page`() {
        val repo = pagingRepo()
        val (vm, _, _) = viewModel(repo)

        assertEquals("init 应已加载首页", pageSize, vm.feedWorks.value.size)

        vm.loadFeed()
        assertEquals(pageSize * 2, vm.feedWorks.value.size)
    }

    @Test
    fun `feed is capped so a long session cannot grow unbounded`() {
        val repo = pagingRepo()
        val (vm, _, _) = viewModel(repo)

        repeat(60) { vm.loadFeed() }

        assertTrue("探索流应被截断，实际 ${vm.feedWorks.value.size}", vm.feedWorks.value.size <= 120)
    }

    @Test
    fun `refresh replaces rather than appends`() {
        val repo = pagingRepo()
        val (vm, _, _) = viewModel(repo)

        vm.loadFeed()
        assertEquals(pageSize * 2, vm.feedWorks.value.size)

        vm.loadFeed(refresh = true)

        assertEquals("刷新应整体替换而非追加", pageSize, vm.feedWorks.value.size)
    }

    @Test
    fun `pull to refresh issues a fresh random seed`() {
        val seeds = mutableListOf<Int?>()
        val repo = TestRepository { _, _, _, _, seed ->
            seeds += seed
            ApiResponse(code = 200, data = worksPage(listOf("w1")))
        }
        val (vm, _, _) = viewModel(repo)
        val seedAtInit = seeds.last()

        vm.refreshFeed()

        assertTrue("下拉刷新应带随机种子以打散顺序，实际种子序列 $seeds", seeds.last() != null)
        assertTrue("刷新应换用新种子而非沿用首页那次的 $seedAtInit", seeds.last() != seedAtInit)
    }

    @Test
    fun `feed failure does not clear the list or crash`() {
        // init 会先打一次请求（精选 + 探索各一次），因此这里在构造之后再换成失败桩，
        // 而不是按调用序号猜哪一次是探索流。
        val repo = TestRepository { _, _, _, _, _ ->
            ApiResponse(code = 200, data = worksPage(listOf("w1")))
        }
        val (vm, _, _) = viewModel(repo)
        assertEquals(listOf("w1"), vm.feedWorks.value.map { it.workId })

        repo.api.onGetWorkList = { _, _, _, _, _ -> ApiResponse(code = 500, message = "服务异常") }
        vm.loadFeed()

        assertEquals("失败不应清空已有内容", listOf("w1"), vm.feedWorks.value.map { it.workId })
        assertFalse(vm.feedLoading.value)
    }

    @Test
    fun `selecting a base url persists it through the node manager`() {
        val repo = pagingRepo()
        val (vm, nodes, _) = viewModel(repo)

        vm.selectBaseUrl("https://mirror.example.com/")

        assertEquals(listOf("https://mirror.example.com/"), nodes.updatedTo)
        assertEquals("https://mirror.example.com/", vm.baseUrl.value)
    }

    @Test
    fun `adding an invalid url is rejected without touching the node manager`() {
        val repo = pagingRepo()
        val (vm, nodes, _) = viewModel(repo)

        vm.addAndSelectUrl("not-a-url")

        assertEquals("非法地址不应触发切换", emptyList<String>(), nodes.updatedTo)
    }

    @Test
    fun `saving a proxy config invalidates the repository and re-applies settings`() {
        val repo = pagingRepo()
        val (vm, nodes, proxy) = viewModel(repo)

        vm.saveProxyConfig(enabled = true, type = "SOCKS", host = "127.0.0.1", port = 1080)

        assertEquals("代理变更必须重建仓库", 1, nodes.invalidated)
        assertEquals("代理变更必须让新配置对新连接生效", 1, proxy.refreshes)
    }
}
