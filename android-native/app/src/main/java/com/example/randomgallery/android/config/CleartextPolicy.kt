package com.example.randomgallery.android.config

import com.example.randomgallery.android.BuildConfig
import java.net.URI

/**
 * 明文（cleartext）HTTP 的放行判定。
 *
 * ## 为什么策略落在代码里而不是 XML
 * release 的网络安全配置在 `android:networkSecurityConfig` 里。NSC 的 `<domain>` 只做
 * **字符串精确匹配 + 子域后缀匹配**（见 AOSP `ApplicationConfig#getConfigForHostname`），
 * 既不支持 CIDR / 网段，也不把 IP 字面量当特例（`XmlConfigSource#parseDomain` 只是
 * trim+lowercase 后存成字符串）。于是「只放行内网 IP」这件事根本无法在 XML 里表达：
 * 内网地址由用户随时填写，不可能逐个枚举。
 *
 * 更要紧的是，只要应用声明了 NSC，manifest 的 `android:usesCleartextTraffic`
 * 就被完全忽略（官方文档：「This flag is ignored on Android 7.0 (API level 24) and above
 * if an Android Network Security Config is present」）。所以早期用的
 * 「manifest flag 作为 IP 字面量的 fallback」是一条不成立的路子 —— 它会让 release 下的
 * 内网节点直接被平台拦掉（OkHttp 报 CLEARTEXT communication not permitted），
 * 而自定义节点正是本应用的核心功能。
 *
 * 因此 NSC 的 `base-config` 必须放开明文，而「只允许内网」这条约束改在这里执行：
 * 校验用户填写的服务地址，公网地址在 release 下一律要求 HTTPS。
 */
object CleartextPolicy {

    /**
     * 该地址是否允许以明文 HTTP 使用。
     *
     * - https：始终允许。
     * - http + 回环/内网：始终允许 —— 用户自建后端与模拟器宿主别名都在这里。
     * - http + 公网：仅 debug 放行，release 拒绝，避免节点地址与请求内容
     *   在公共 Wi-Fi 下被嗅探篡改。
     *
     * @param permitPublicCleartext 默认取构建变体；显式传入是为了让两个分支都能在
     *   单测里覆盖（单测跑在 debug 变体下，默认值看不到 release 的行为）。
     */
    fun permitsHttpUrl(
        url: String,
        permitPublicCleartext: Boolean = BuildConfig.PERMIT_PUBLIC_CLEARTEXT,
    ): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        // 无主机的地址根本不可用，不能因为"公网明文放行"这一条就被判为允许
        if (uri.host.isNullOrBlank()) return false
        if (scheme == "https") return true
        return isPrivateHost(url) || permitPublicCleartext
    }

    /**
     * 回环 / mDNS / 内网 IPv4 判定。
     * 10.x 覆盖 Android 模拟器宿主别名 10.0.2.2。
     */
    fun isPrivateHost(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        if (host == "localhost" || host == "127.0.0.1" || host == "[::1]" || host == "::1") return true
        if (host.endsWith(".local")) return true
        val parts = host.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        val a = parts[0]
        val b = parts[1]
        return a == 10 ||
            (a == 192 && b == 168) ||
            (a == 172 && b in 16..31) ||
            (a == 169 && b == 254)   // 链路本地，常见于直连设备
    }
}
