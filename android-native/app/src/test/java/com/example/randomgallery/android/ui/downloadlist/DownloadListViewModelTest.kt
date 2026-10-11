package com.example.randomgallery.android.ui.downloadlist

import androidx.lifecycle.SavedStateHandle
import com.example.randomgallery.android.data.model.ApiResponse
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `DownloadListViewModel` 纯 JVM 单测。
 *
 * 重点覆盖两处曾经出错的分页状态：
 * 1. 分页/筛选状态此前是普通字段，旋转屏幕后 page 归 1、seed 重随机，列表整体重排；
 * 2. 带筛选条件时不应再传随机种子（否则同一筛选每次顺序都不同）。
 * 这两点都靠 SavedStateHandle，因此测试直接检查 handle 的存取。
 *
 * `init` 自身会加载第一页，因此所有断言都以此为基线。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    /** 每页返回一组可辨识的作品，方便断言追加/替换行为。 */
    private fun repoFor(pages: (Int) -> List<String> = { p -> listOf("w$p") }): TestRepository =
        TestRepository { page, _, _, _, _ ->
            ApiResponse(code = 200, data = worksPage(pages(page)))
        }

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        repo: TestRepository = repoFor(),
    ) = DownloadListViewModel(repo.provider, handle)

    @Test
    fun `first page loads and advances the page key`() {
        val repo = repoFor { listOf("w1", "w2") }
        val handle = SavedStateHandle()

        val vm = viewModel(handle, repo)

        assertEquals(listOf("w1", "w2"), vm.works.value.map { it.workId })
        assertEquals(listOf(1), repo.requestedPages)
        assertEquals("加载完第 1 页后指针应停在第 2 页", 2, handle.get<Int>("dlPage"))
    }

    @Test
    fun `loadMore appends the next page`() {
        val repo = repoFor { p -> if (p == 1) listOf("w1") else listOf("w2") }
        val vm = viewModel(repo = repo)
        assertEquals(listOf("w1"), vm.works.value.map { it.workId })

        vm.loadMore()

        assertEquals(listOf(1, 2), repo.requestedPages)
        assertEquals(listOf("w1", "w2"), vm.works.value.map { it.workId })
    }

    @Test
    fun `short page stops further paging`() {
        // hasMore=false：首页即末页
        val repo = TestRepository { _, _, _, _, _ ->
            ApiResponse(code = 200, data = worksPage(listOf("w1"), hasMore = false))
        }
        val vm = viewModel(repo = repo)

        vm.loadMore()
        vm.loadMore()

        assertEquals(listOf(1), repo.requestedPages)
    }

    @Test
    fun `saved page and seed survive recreation instead of resetting`() {
        val repo = repoFor()
        val handle = SavedStateHandle()
        val first = viewModel(handle, repo)
        first.loadMore()
        assertEquals(listOf(1, 2), repo.requestedPages)
        val seedBefore = handle.get<Int>("dlSeed")

        // 模拟旋转屏幕：同一个 SavedStateHandle 交给新实例
        viewModel(handle, repo)

        assertEquals(
            "重建后应从第 3 页继续，而不是回到第 1 页",
            listOf(1, 2, 3),
            repo.requestedPages,
        )
        assertEquals("重建后随机种子必须保持，否则列表顺序会变", seedBefore, handle.get<Int>("dlSeed"))
    }

    @Test
    fun `filters coming from navigation override saved values`() {
        val handle = SavedStateHandle(
            mapOf(
                "dlAuthorId" to "oldAuthor",
                "filterAuthorId" to "newAuthor",
                "filterKeyword" to "风景",
            )
        )

        val vm = viewModel(handle, repoFor())

        assertEquals("导航参数应覆盖已保存值", "newAuthor", vm.authorId)
        assertEquals("风景", vm.keyword)
    }

    @Test
    fun `filtered request omits the random seed`() {
        // 带筛选时传 seed 会让同一筛选条件的每次排序都不同
        val seeds = mutableListOf<Int?>()
        val repo = TestRepository { _, _, _, _, seed ->
            seeds += seed
            ApiResponse(code = 200, data = worksPage(listOf("w1")))
        }
        val handle = SavedStateHandle(mapOf("filterAuthorId" to "a1"))

        viewModel(handle, repo)

        assertEquals(listOf<Int?>(null), seeds)
    }

    @Test
    fun `unfiltered request carries the persisted seed`() {
        val seeds = mutableListOf<Int?>()
        val repo = TestRepository { _, _, _, _, seed ->
            seeds += seed
            ApiResponse(code = 200, data = worksPage(listOf("w1")))
        }

        viewModel(repo = repo)

        assertTrue("无筛选时应带随机种子以打散顺序", seeds.single() != null)
    }

    @Test
    fun `failure surfaces the message and clears loading`() {
        val repo = TestRepository { _, _, _, _, _ ->
            ApiResponse<com.example.randomgallery.android.data.model.XhsWorkPageVO>(
                code = 500,
                message = "服务异常",
            )
        }
        val vm = viewModel(repo = repo)

        assertEquals("服务异常", vm.error.value)
        assertFalse(vm.loading.value)
        assertEquals(emptyList<String>(), vm.works.value.map { it.workId })
    }

    @Test
    fun `works list is capped so a long session cannot grow unbounded`() {
        // MAX_WORKS = 200：每次返回 10 条且 hasMore 恒真
        var n = 0
        val repo = TestRepository { _, _, _, _, _ ->
            ApiResponse(code = 200, data = worksPage((1..10).map { "w${n++}" }, hasMore = true))
        }
        val vm = viewModel(repo = repo)

        repeat(40) { vm.loadMore() }

        assertTrue("列表应被截断在 200 条以内，实际 ${vm.works.value.size}", vm.works.value.size <= 200)
    }

    @Test
    fun `removeWork drops the entry locally`() {
        val repo = repoFor { listOf("w1", "w2") }
        val vm = viewModel(repo = repo)

        vm.removeWork("w1")

        assertEquals(listOf("w2"), vm.works.value.map { it.workId })
    }

    @Test
    fun `resetFilters clears the filter state`() {
        val handle = SavedStateHandle(
            mapOf("filterAuthorId" to "a1", "filterKeyword" to "风景")
        )
        val vm = viewModel(handle, repoFor())

        vm.resetFilters()

        assertNull(vm.authorId)
        assertNull(vm.keyword)
        assertNull(vm.tagId)
    }
}
