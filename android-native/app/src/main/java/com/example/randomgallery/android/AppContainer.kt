package com.example.randomgallery.android

import android.content.Context
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.local.DatabaseModule
import com.example.randomgallery.android.data.network.NetworkModule
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppContainer {

    // 受 SupervisorJob 管控的初始化作用域：单异常不会取消整个作用域，也不会逃逸为未捕获崩溃。
    private val initScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }
    )

    @Volatile private var repository: GalleryRepository? = null
    @Volatile private var repositoryBaseUrl: String? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        // 先用默认 baseUrl 立即返回（不阻塞 Main 线程），随后异步读 DataStore 再切换
        BaseUrlConfig.update(BaseUrlConfig.resolve(null, BuildConfig.DEFAULT_BASE_URL))
        initScope.launch {
            val savedBaseUrl = AppPrefs(appContext).getBaseUrl()
            val resolved = BaseUrlConfig.resolve(savedBaseUrl, BuildConfig.DEFAULT_BASE_URL)
            if (resolved != BaseUrlConfig.current()) {
                BaseUrlConfig.update(resolved)
                clearRepository()
            }
        }
    }

    fun currentBaseUrl(): String = BaseUrlConfig.current()

    suspend fun updateBaseUrl(context: Context, baseUrl: String) {
        val appContext = context.applicationContext
        val resolved = BaseUrlConfig.resolve(baseUrl, BuildConfig.DEFAULT_BASE_URL)
        AppPrefs(appContext).saveBaseUrl(resolved)
        BaseUrlConfig.update(resolved)
        NetworkModule.clearHttpCache()
        repository?.clearCache()
        clearRepository()
    }

    fun clearRepository() {
        synchronized(this) {
            repository = null
            repositoryBaseUrl = null
        }
        NetworkModule.resetOkHttpClient()
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
                        context = appContext,
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
