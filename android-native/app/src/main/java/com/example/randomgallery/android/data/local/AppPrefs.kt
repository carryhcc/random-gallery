package com.example.randomgallery.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

/**
 * 应用偏好（DataStore 支撑），[AppSettings] 的唯一实现。
 *
 * 下游一律依赖 [AppSettings] / [Preferences] 接口而非本类：DataStore 在 JVM 上无法实例化
 * （依赖 Android Context），直接依赖本类会让任何下游都无法在无设备环境下测试。
 * 全项目只有在 DI 模块里 `AppPrefs(context)` 这一次具体构造。
 */
class AppPrefs(private val context: Context) : AppSettings {

    companion object {
        private val KEY_ENV = stringPreferencesKey("env")
        private val KEY_PRIVACY = booleanPreferencesKey("privacy_mode")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_VIEW_MODE = stringPreferencesKey("download_view_mode")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_AUTO_READ_CLIPBOARD = booleanPreferencesKey("auto_read_clipboard")
        // URL 列表以换行符分隔存储
        private val KEY_URL_LIST = stringPreferencesKey("url_list")
        private val KEY_XHS_COOKIE = stringPreferencesKey("xhs_cookie")

        private val KEY_SPACE_MODE = stringPreferencesKey("app_space_mode")
        
        // 自定义网络代理配置
        private val KEY_PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        private val KEY_PROXY_TYPE = stringPreferencesKey("proxy_type") // "HTTP" / "SOCKS"
        private val KEY_PROXY_HOST = stringPreferencesKey("proxy_host")
        private val KEY_PROXY_PORT = androidx.datastore.preferences.core.intPreferencesKey("proxy_port")

        private const val URL_SEPARATOR = "\n"
    }

    override val proxyEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROXY_ENABLED] ?: false }
    override val proxyTypeFlow: Flow<String> = context.dataStore.data.map { it[KEY_PROXY_TYPE] ?: "HTTP" }
    override val proxyHostFlow: Flow<String> = context.dataStore.data.map { it[KEY_PROXY_HOST] ?: "127.0.0.1" }
    override val proxyPortFlow: Flow<Int> = context.dataStore.data.map { it[KEY_PROXY_PORT] ?: 7890 }

    override val spaceModeFlow: Flow<String> = context.dataStore.data.map { it[KEY_SPACE_MODE] ?: "explore" }
    override val envFlow: Flow<String> = context.dataStore.data.map { it[KEY_ENV] ?: "dev" }
    override val privacyFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_PRIVACY] ?: true }
    override val darkModeFlow: Flow<String> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: "system" }
    override val viewModeFlow: Flow<String> = context.dataStore.data.map { it[KEY_VIEW_MODE] ?: "single" }
    override val baseUrlFlow: Flow<String?> = context.dataStore.data.map { it[KEY_BASE_URL] }
    override val autoReadClipboardFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_READ_CLIPBOARD] ?: false }
    override val xhsCookieFlow: Flow<String> = context.dataStore.data.map { it[KEY_XHS_COOKIE] ?: "" }
    override val urlListFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_URL_LIST]?.split(URL_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
    }

    override suspend fun saveProxyConfig(enabled: Boolean, type: String, host: String, port: Int) {
        context.dataStore.edit {
            it[KEY_PROXY_ENABLED] = enabled
            it[KEY_PROXY_TYPE] = type
            it[KEY_PROXY_HOST] = host
            it[KEY_PROXY_PORT] = port
        }
    }

    override suspend fun saveEnv(env: String) {
        context.dataStore.edit { it[KEY_ENV] = env }
    }

    override suspend fun savePrivacy(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PRIVACY] = enabled }
    }

    override suspend fun saveSpaceMode(mode: String) {
        context.dataStore.edit { it[KEY_SPACE_MODE] = mode }
    }

    override suspend fun saveDarkMode(mode: String) {
        context.dataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    override suspend fun saveViewMode(mode: String) {
        context.dataStore.edit { it[KEY_VIEW_MODE] = mode }
    }

    override suspend fun saveBaseUrl(baseUrl: String?) {
        context.dataStore.edit {
            if (baseUrl.isNullOrBlank()) {
                it.remove(KEY_BASE_URL)
            } else {
                it[KEY_BASE_URL] = baseUrl
            }
        }
    }

    override suspend fun saveAutoReadClipboard(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_READ_CLIPBOARD] = enabled }
    }

    override suspend fun saveXhsCookie(cookie: String) {
        context.dataStore.edit {
            if (cookie.isBlank()) {
                it.remove(KEY_XHS_COOKIE)
            } else {
                it[KEY_XHS_COOKIE] = cookie.trim()
            }
        }
    }

    override suspend fun getBaseUrl(): String? = baseUrlFlow.first()

    override suspend fun saveUrlList(urls: List<String>) {
        context.dataStore.edit { it[KEY_URL_LIST] = urls.joinToString(URL_SEPARATOR) }
    }

    override suspend fun getUrlList(): List<String> = urlListFlow.first()
}
