package com.example.randomgallery.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderTest {

    @Test
    fun `url 自带文件名和扩展名时直接沿用`() {
        assertEquals(
            "abc123.png",
            buildDownloadFileName("https://cdn.example.com/path/abc123.png?x=1", "rg", "jpg")
        )
    }

    @Test
    fun `url 末段无扩展名时补默认扩展名`() {
        assertEquals(
            "abc123.jpg",
            buildDownloadFileName("https://cdn.example.com/path/abc123", "rg", "jpg")
        )
    }

    @Test
    fun `url 无有效末段时用前缀加时间戳`() {
        val name = buildDownloadFileName("https://cdn.example.com/", "rg", "mp4")
        assertTrue(name.startsWith("rg_"))
        assertTrue(name.endsWith(".mp4"))
    }

    @Test
    fun `默认扩展名带点号也能正确处理`() {
        assertEquals(
            "abc.jpg",
            buildDownloadFileName("https://cdn.example.com/abc", "rg", ".jpg")
        )
    }
}
