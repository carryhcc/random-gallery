package com.example.randomgallery.android.data.local

import kotlinx.coroutines.flow.Flow

/**
 * [GalleryRepository] 所需的最小偏好读写面。
 *
 * 抽成接口而非直接依赖 [AppPrefs]，是为了让仓库能在纯 JVM 测试中构造：
 * AppPrefs 底层是 DataStore，必须有 Android Context 才能实例化，
 * 否则任何依赖仓库的 ViewModel 都无法在无设备环境下测试。
 */
interface Preferences {
    val envFlow: Flow<String>
    val privacyFlow: Flow<Boolean>
    val viewModeFlow: Flow<String>
    val baseUrlFlow: Flow<String?>
    val autoReadClipboardFlow: Flow<Boolean>
    val urlListFlow: Flow<List<String>>
    val xhsCookieFlow: Flow<String>

    suspend fun saveEnv(env: String)
    suspend fun savePrivacy(enabled: Boolean)
    suspend fun saveViewMode(mode: String)
    suspend fun saveAutoReadClipboard(enabled: Boolean)
    suspend fun saveXhsCookie(cookie: String)
}
