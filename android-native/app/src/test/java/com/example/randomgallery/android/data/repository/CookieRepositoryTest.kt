package com.example.randomgallery.android.data.repository

import com.example.randomgallery.android.testing.TestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * XHS Cookie 持久化的纯 JVM 单测。
 *
 * 走真实的 [GalleryRepository] + 内存版 [com.example.randomgallery.android.testing.FakePreferences]，
 * 验证"保存后 flow 立即反映新值"以及"空白输入等价于清除"这两条语义。
 */
class CookieRepositoryTest {

    @Test
    fun `saving a cookie is emitted by the flow`() = runTest {
        val repository = TestRepository().repository

        repository.saveXhsCookie("a1=abc; web_session=xyz")

        assertEquals("a1=abc; web_session=xyz", repository.xhsCookieFlow.first())
    }

    @Test
    fun `saving a blank cookie clears it`() = runTest {
        val repository = TestRepository().repository
        repository.saveXhsCookie("a1=abc; web_session=xyz")

        repository.saveXhsCookie("  ")

        assertEquals("", repository.xhsCookieFlow.first())
    }
}
