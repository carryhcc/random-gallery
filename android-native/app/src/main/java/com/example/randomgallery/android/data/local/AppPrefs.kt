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

class AppPrefs(private val context: Context) {

    companion object {
        private val KEY_ENV = stringPreferencesKey("env")
        private val KEY_PRIVACY = booleanPreferencesKey("privacy_mode")
        private val KEY_VIEW_MODE = stringPreferencesKey("download_view_mode")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_AUTO_READ_CLIPBOARD = booleanPreferencesKey("auto_read_clipboard")
        // URL 列表以换行符分隔存储
        private val KEY_URL_LIST = stringPreferencesKey("url_list")

        private const val URL_SEPARATOR = "\n"
    }

    val envFlow: Flow<String> = context.dataStore.data.map { it[KEY_ENV] ?: "dev" }
    val privacyFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_PRIVACY] ?: true }
    val viewModeFlow: Flow<String> = context.dataStore.data.map { it[KEY_VIEW_MODE] ?: "single" }
    val baseUrlFlow: Flow<String?> = context.dataStore.data.map { it[KEY_BASE_URL] }
    val autoReadClipboardFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_READ_CLIPBOARD] ?: false }
    val urlListFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_URL_LIST]?.split(URL_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveEnv(env: String) {
        context.dataStore.edit { it[KEY_ENV] = env }
    }

    suspend fun savePrivacy(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PRIVACY] = enabled }
    }

    suspend fun saveViewMode(mode: String) {
        context.dataStore.edit { it[KEY_VIEW_MODE] = mode }
    }

    suspend fun saveBaseUrl(baseUrl: String?) {
        context.dataStore.edit {
            if (baseUrl.isNullOrBlank()) {
                it.remove(KEY_BASE_URL)
            } else {
                it[KEY_BASE_URL] = baseUrl
            }
        }
    }

    suspend fun saveAutoReadClipboard(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_READ_CLIPBOARD] = enabled }
    }

    suspend fun getBaseUrl(): String? = baseUrlFlow.first()

    suspend fun saveUrlList(urls: List<String>) {
        context.dataStore.edit { it[KEY_URL_LIST] = urls.joinToString(URL_SEPARATOR) }
    }

    suspend fun getUrlList(): List<String> = urlListFlow.first()
}
