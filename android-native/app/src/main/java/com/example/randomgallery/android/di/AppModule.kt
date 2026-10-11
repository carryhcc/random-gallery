package com.example.randomgallery.android.di

import android.content.Context
import androidx.room.Room
import com.example.randomgallery.android.BuildConfig
import com.example.randomgallery.android.data.local.AppDatabase
import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.local.AppSettings
import com.example.randomgallery.android.data.local.CachedPayloadDao
import com.example.randomgallery.android.data.network.NetworkModule
import com.example.randomgallery.android.data.network.ProxyController
import com.example.randomgallery.android.data.network.ProxySettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 应用级依赖图。
 *
 * 迁移前这些对象由 `AppContainer` 手工装配：每个 ViewModel 拿 Context 再 `AppContainer.repository(context)`
 * 反查，依赖关系只存在于调用点的字符串里，编译器无从校验；测试也无法替换任何一层。
 * 现在全部改为构造器注入，依赖图由 Hilt 在编译期生成并校验。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 全项目唯一的具体构造点。下游一律注入 [AppSettings]（或其父接口
     * `Preferences`），于是 DataStore 的不可测性被挡在这一层之外。
     */
    @Provides
    @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings = AppPrefs(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "random_gallery_native.db"
        ).build()

    @Provides
    @Singleton
    fun provideCacheDao(database: AppDatabase): CachedPayloadDao = database.cacheDao()

    /**
     * 取用能力而非实例：见 [RepositoryProvider] 的说明。
     * 不标 `@Singleton`，因为这个 lambda 无状态，每次构造同样廉价。
     */
    @Provides
    fun provideRepositoryProvider(manager: BaseUrlManager): RepositoryProvider =
        RepositoryProvider { manager.repository() }

    /** 需要改服务地址的页面（首页设置面板）用它；其余用更窄的 [RepositoryProvider]。 */
    @Provides
    fun provideNodeManager(manager: BaseUrlManager): NodeManager = manager

    @Provides
    fun provideProxySettings(controller: ProxyController): ProxySettings = controller

    /**
     * 全应用共享一个 OkHttpClient（含磁盘缓存与连接池）。
     *
     * 这里把 [BaseUrlManager] 注入进来是为了让节点故障切换拦截器能在请求失败时就地改写目标节点；
     * 该依赖不回过来读 OkHttpClient（[BaseUrlManager] 用的是 `Lazy`），因此不构成构造期循环。
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        baseUrlManager: BaseUrlManager,
    ): OkHttpClient = NetworkModule.createOkHttpClient(
        context = context,
        enableHttpLogging = BuildConfig.ENABLE_HTTP_LOGGING,
        baseUrlManager = baseUrlManager,
    )
}
