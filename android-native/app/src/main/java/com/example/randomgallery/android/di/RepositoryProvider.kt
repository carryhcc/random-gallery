package com.example.randomgallery.android.di

import com.example.randomgallery.android.data.repository.GalleryRepository

/**
 * 「按需取当前仓库」的最小能力。
 *
 * ViewModel 不能持有 [GalleryRepository] 实例：换节点后旧 ViewModel 仍活在返回栈里，
 * 必须在下一次调用时拿到指向新地址的仓库。因此这里传的是取用能力而非实例本身。
 *
 * 单独抽成接口是为了让 ViewModel 依赖最小面，纯 JVM 单测可以传一个返回假仓库的实现，
 * 无需构造 Context / OkHttpClient。
 */
fun interface RepositoryProvider {
    fun repository(): GalleryRepository
}

/**
 * 在 [RepositoryProvider] 之上追加服务地址的读写。
 *
 * 只有首页（设置面板里能改地址、切节点）需要，其余 ViewModel 用更窄的
 * [RepositoryProvider] 即可 —— 依赖面越小，被测试替换的成本越低。
 */
interface NodeManager : RepositoryProvider {

    /** 当前生效的服务地址。 */
    fun currentBaseUrl(): String

    /** 切换服务地址：持久化 + 重建仓库。 */
    suspend fun updateBaseUrl(rawUrl: String)

    /** 丢弃当前仓库（代理配置变更后调用）。 */
    fun invalidateRepository()
}
