package com.example.randomgallery.android.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiNodeSwitcherTest {

    @Test
    fun `local node detection covers localhost and private ranges`() {
        assertTrue(ApiNodeSwitcher.isLocalNode("http://localhost:8086/"))
        assertTrue(ApiNodeSwitcher.isLocalNode("http://192.168.1.7:8086/"))
        assertTrue(ApiNodeSwitcher.isLocalNode("http://10.0.2.2:8086/")) // 模拟器宿主别名
        assertTrue(ApiNodeSwitcher.isLocalNode("http://172.16.0.5:8086/"))
        assertTrue(ApiNodeSwitcher.isLocalNode("http://nas.local:8086/"))
    }

    @Test
    fun `remote node detection rejects public hosts`() {
        assertFalse(ApiNodeSwitcher.isLocalNode("https://gallery.example.com/"))
        assertFalse(ApiNodeSwitcher.isLocalNode("http://8.8.8.8:8086/"))
        assertFalse(ApiNodeSwitcher.isLocalNode("http://172.32.0.1:8086/")) // 172 超出私有段
        assertFalse(ApiNodeSwitcher.isLocalNode("not a url"))
    }

    @Test
    fun `ordering puts local nodes first and keeps relative order`() {
        val ordered = ApiNodeSwitcher.orderLocalFirst(
            listOf(
                "https://a.example.com/",
                "http://192.168.1.5:8086/",
                "https://b.example.com/",
                "http://10.0.0.2:8086/"
            )
        )
        assertEquals(
            listOf(
                "http://192.168.1.5:8086/",
                "http://10.0.0.2:8086/",
                "https://a.example.com/",
                "https://b.example.com/"
            ),
            ordered
        )
    }

    @Test
    fun `merge dedups and sanitizes while keeping first occurrence`() {
        val merged = ApiNodeSwitcher.mergeCandidates(
            saved = "http://192.168.1.5:8086/",
            nodeList = listOf("http://192.168.1.5:8086/", "https://a.example.com"),
            default = "https://a.example.com/"
        )
        assertEquals(
            listOf("http://192.168.1.5:8086/", "https://a.example.com/"),
            merged
        )
    }
}
