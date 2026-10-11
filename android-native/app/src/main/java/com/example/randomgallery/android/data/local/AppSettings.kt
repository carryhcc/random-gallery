package com.example.randomgallery.android.data.local

import kotlinx.coroutines.flow.Flow

/**
 * 应用设置的全量读写面。
 *
 * 在 [Preferences]（仓库所需最小面）之上补齐 UI 与生命周期组件用到的部分：
 * 代理配置、暗色模式、空间模式、服务地址与地址列表。
 *
 * 存在的意义与 [Preferences] 相同：让依赖方能声明自己真正需要的东西，
 * 而不是被强制依赖 DataStore 支撑的 [AppPrefs]（JVM 上无法实例化）。
 * 注入点一律用本接口，[AppPrefs] 只在 DI 模块里出现一次。
 */
interface AppSettings : Preferences {
    val darkModeFlow: Flow<String>
    val spaceModeFlow: Flow<String>

    val proxyEnabledFlow: Flow<Boolean>
    val proxyTypeFlow: Flow<String>
    val proxyHostFlow: Flow<String>
    val proxyPortFlow: Flow<Int>

    suspend fun saveDarkMode(mode: String)
    suspend fun saveSpaceMode(mode: String)
    suspend fun saveProxyConfig(enabled: Boolean, type: String, host: String, port: Int)

    suspend fun getBaseUrl(): String?
    suspend fun saveBaseUrl(baseUrl: String?)

    suspend fun getUrlList(): List<String>
    suspend fun saveUrlList(urls: List<String>)
}
