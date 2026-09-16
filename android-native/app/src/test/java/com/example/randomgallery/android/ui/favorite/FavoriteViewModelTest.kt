package com.example.randomgallery.android.ui.favorite

import com.example.randomgallery.android.data.model.RandomGifVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * REQ-09：`FavoriteViewModel` 纯 JVM 单测。
 *
 * ViewModel 通过构造函数接收两个 suspend 函数，因此不需要 Room / Context / 网络。
 * `viewModelScope` 依赖 `Dispatchers.Main`，用 coroutines-test 的 `setMain` 替换掉。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun gif(id: Long) = RandomGifVO(
        id = id,
        workId = "w$id",
        workTitle = "作品 $id",
        mediaUrl = "https://cdn.example.com/$id.mp4"
    )

    private fun viewModel(
        load: suspend (Int, Int) -> Result<List<RandomGifVO>> = { _, _ -> Result.success(emptyList()) },
        toggle: suspend (Long, String) -> Result<Boolean> = { _, _ -> Result.success(false) }
    ) = FavoriteViewModel(loadFavorites = load, toggleFavorite = toggle)

    @Test
    fun `load success fills items and clears error`() {
        val vm = viewModel(load = { _, _ -> Result.success(listOf(gif(1), gif(2))) })

        assertEquals(listOf(1L, 2L), vm.items.value.map { it.id })
        assertNull(vm.error.value)
        assertFalse(vm.loading.value)
    }

    @Test
    fun `load failure surfaces error and keeps list empty`() {
        val vm = viewModel(load = { _, _ -> Result.failure(RuntimeException("boom")) })

        assertEquals(emptyList<Long>(), vm.items.value.map { it.id })
        assertEquals("boom", vm.error.value)
        assertFalse(vm.loading.value)
    }

    @Test
    fun `unfavorite removes the item immediately`() {
        val vm = viewModel(
            load = { _, _ -> Result.success(listOf(gif(1), gif(2))) },
            toggle = { _, _ -> Result.success(false) }
        )
        assertEquals(listOf(1L, 2L), vm.items.value.map { it.id })

        vm.removeFavorite(1)

        assertEquals(listOf(2L), vm.items.value.map { it.id })
    }

    @Test
    fun `toggle returning favorited keeps the item`() {
        val vm = viewModel(
            load = { _, _ -> Result.success(listOf(gif(1), gif(2))) },
            toggle = { _, _ -> Result.success(true) }
        )

        vm.removeFavorite(1)

        assertEquals(listOf(1L, 2L), vm.items.value.map { it.id })
    }

    @Test
    fun `full page keeps paging and appends the next page`() {
        val requestedPages = mutableListOf<Int>()
        val vm = viewModel(load = { page, _ ->
            requestedPages += page
            if (page == 1) Result.success((1L..20L).map { gif(it) }) else Result.success(listOf(gif(21)))
        })
        assertEquals((1L..20L).toList(), vm.items.value.map { it.id })

        vm.loadMore()

        assertEquals(listOf(1, 2), requestedPages)
        assertEquals(listOf(21L), vm.items.value.drop(20).map { it.id })
    }

    @Test
    fun `triggering loadMore twice from a short page does not refetch`() {
        val requestedPages = mutableListOf<Int>()
        val vm = viewModel(load = { page, _ ->
            requestedPages += page
            Result.success(listOf(gif(page.toLong())))
        })

        vm.loadMore()
        vm.loadMore()

        // 首页只返回 1 条（< PAGE_SIZE），hasMore 应为 false，不应再请求第 2 页
        assertEquals(listOf(1), requestedPages)
    }
}
