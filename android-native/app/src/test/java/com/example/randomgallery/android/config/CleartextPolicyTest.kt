package com.example.randomgallery.android.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `CleartextPolicy` 单测。
 *
 * 这层存在的理由是 NSC 表达不了「只放行内网」（见该类文档），因此它成了 release 下
 * 明文策略的唯一执行点 —— 一旦判错，要么内网节点全被自己拦掉（功能坏），
 * 要么公网明文被放行（安全坏）。两个方向都要钉住。
 *
 * `permitsHttpUrl` 的第二个参数显式传入，因为单测运行在 debug 变体下，
 * 默认值恒为 true，看不到 release 的行为。
 */
class CleartextPolicyTest {

    // ── https 恒放行 ────────────────────────────────────────────────
    @Test
    fun `https is always permitted`() {
        assertTrue(CleartextPolicy.permitsHttpUrl("https://gallery.example.com/"))
        assertTrue(CleartextPolicy.permitsHttpUrl("https://8.8.8.8:8443/"))
    }

    // ── 非 http(s) 一律拒绝 ─────────────────────────────────────────
    @Test
    fun `non-http schemes are rejected`() {
        assertFalse(CleartextPolicy.permitsHttpUrl("ftp://192.168.1.7:8086/"))
        assertFalse(CleartextPolicy.permitsHttpUrl("ws://192.168.1.7:8086/"))
    }

    // ── 内网明文：release 也必须放行 ────────────────────────────────
    @Test
    fun `private hosts permit cleartext even in release`() {
        val release = false
        assertTrue(CleartextPolicy.permitsHttpUrl("http://localhost:8086/", release))
        assertTrue(CleartextPolicy.permitsHttpUrl("http://127.0.0.1:8086/", release))
        assertTrue(CleartextPolicy.permitsHttpUrl("http://10.0.2.2:8086/", release))   // 模拟器宿主
        assertTrue(CleartextPolicy.permitsHttpUrl("http://192.168.1.7:8086/", release))
        assertTrue(CleartextPolicy.permitsHttpUrl("http://172.16.0.5:8086/", release))
        assertTrue(CleartextPolicy.permitsHttpUrl("http://nas.local:8086/", release))
    }

    // ── 公网明文：release 拒绝、debug 放行 ──────────────────────────
    @Test
    fun `public hosts require https in release`() {
        val release = false
        assertFalse(CleartextPolicy.permitsHttpUrl("http://gallery.example.com/", release))
        assertFalse(CleartextPolicy.permitsHttpUrl("http://8.8.8.8:8086/", release))
        // 172 私有段到 172.31 为止，172.32 起是公网
        assertFalse(CleartextPolicy.permitsHttpUrl("http://172.32.0.1:8086/", release))
        // 198.18/15 等既非私有也非回环
        assertFalse(CleartextPolicy.permitsHttpUrl("http://198.18.0.1:8086/", release))
    }

    @Test
    fun `public hosts permit cleartext in debug`() {
        assertTrue(CleartextPolicy.permitsHttpUrl("http://gallery.example.com/", permitPublicCleartext = true))
    }

    // ── 畸形输入 ────────────────────────────────────────────────────
    @Test
    fun `malformed input is rejected`() {
        assertFalse(CleartextPolicy.permitsHttpUrl("not a url"))
        assertFalse(CleartextPolicy.permitsHttpUrl(""))
        assertFalse(CleartextPolicy.permitsHttpUrl("http:///path"))
    }

    // ── 主机判定本身 ────────────────────────────────────────────────
    @Test
    fun `out-of-range octets are not treated as private`() {
        assertFalse(CleartextPolicy.isPrivateHost("http://999.168.1.7:8086/"))
        assertFalse(CleartextPolicy.isPrivateHost("http://192.168.1:8086/"))
    }

    @Test
    fun `link-local addresses are treated as private`() {
        // 直连设备（USB 网络共享等）常见，节点探测与明文放行应当一致
        assertTrue(CleartextPolicy.isPrivateHost("http://169.254.10.20:8086/"))
    }
}
