package com.example.randomgallery.android

import android.content.Context
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.local.DatabaseModule
import com.example.randomgallery.android.data.network.NetworkModule
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

object AppContainer {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var repository: GalleryRepository? = null
    @Volatile private var repositoryBaseUrl: String? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        // Application.onCreate() 本身在 Main 线程，UI 尚未渲染，短暂阻塞读取 DataStore 是安全的
        val savedBaseUrl = runBlocking(Dispatchers.IO) { AppPrefs(appContext).getBaseUrl() }
        BaseUrlConfig.update(BaseUrlConfig.resolve(savedBaseUrl, BuildConfig.DEFAULT_BASE_URL))
    }

    fun currentBaseUrl(): String = BaseUrlConfig.current()

    suspend fun updateBaseUrl(context: Context, baseUrl: String) {
        val appContext = context.applicationContext
        val resolved = BaseUrlConfig.resolve(baseUrl, BuildConfig.DEFAULT_BASE_URL)
        AppPrefs(appContext).saveBaseUrl(resolved)
        BaseUrlConfig.update(resolved)
        clearRepository()
    }

    private fun clearRepository() {
        synchronized(this) {
            repository = null
            repositoryBaseUrl = null
        }
    }

    fun repository(context: Context): GalleryRepository {
        val appContext = context.applicationContext
        val baseUrl = BaseUrlConfig.current()
        val currentRepository = repository
        if (currentRepository != null && repositoryBaseUrl == baseUrl) {
            return currentRepository
        }
        return synchronized(this) {
            val curr = repository
            if (curr != null && repositoryBaseUrl == baseUrl) {
                curr
            } else {
                GalleryRepository(
                    api = NetworkModule.createApiService(
                        cacheDir = appContext.cacheDir,
                        baseUrl = baseUrl,
                        enableHttpLogging = BuildConfig.ENABLE_HTTP_LOGGING
                    ),
                    cacheDao = DatabaseModule.provideDatabase(appContext).cacheDao(),
                    prefs = AppPrefs(appContext)
                ).also {
                    repository = it
                    repositoryBaseUrl = baseUrl
                }
            }
        }
    }
}
