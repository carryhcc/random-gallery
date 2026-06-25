package com.example.randomgallery.android.data.repository

import com.example.randomgallery.android.data.local.AppPrefs
import com.example.randomgallery.android.data.local.CachedPayload
import com.example.randomgallery.android.data.local.CachedPayloadDao
import com.example.randomgallery.android.data.model.*
import com.example.randomgallery.android.config.BaseUrlConfig
import com.example.randomgallery.android.data.network.ApiService
import com.example.randomgallery.android.data.network.NetworkModule
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GalleryRepository(
    private val api: ApiService,
    private val cacheDao: CachedPayloadDao,
    private val prefs: AppPrefs
) {

    private val moshi = NetworkModule.moshi

    val envFlow: Flow<String> = prefs.envFlow
    val privacyFlow: Flow<Boolean> = prefs.privacyFlow
    val viewModeFlow: Flow<String> = prefs.viewModeFlow
    val baseUrlFlow: Flow<String> = prefs.baseUrlFlow.map { BaseUrlConfig.resolve(it, BaseUrlConfig.current()) }
    val autoReadClipboardFlow: Flow<Boolean> = prefs.autoReadClipboardFlow
    val urlListFlow: Flow<List<String>> = prefs.urlListFlow

    suspend fun getRandomPic(): Result<PicVO> = try {
        val res = api.getRandomPic()
        if (res.code == 200 && res.data != null) {
            cache("random_pic", moshi.adapter(PicVO::class.java).toJson(res.data))
            Result.success(res.data)
        } else {
            loadRandomPicFromCache() ?: Result.failure(Exception(res.message ?: "加载失败"))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        loadRandomPicFromCache() ?: Result.failure(e)
    }

    private suspend fun loadRandomPicFromCache(): Result<PicVO>? {
        val payload = cacheDao.findByKey("random_pic")?.payload ?: return null
        val value = moshi.adapter(PicVO::class.java).fromJson(payload) ?: return null
        return Result.success(value)
    }

    suspend fun getPicList(groupId: Long, page: Int, size: Int): Result<List<PicVO>> = try {
        val res = api.getPicList(groupId = groupId, pageIndex = page, pageSize = size)
        if (res.code == 200) Result.success(res.data ?: emptyList())
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getRandomGroupInfo(groupId: Long? = null): Result<GroupVO> = try {
        val res = api.getRandomGroupInfo(groupId)
        if (res.code == 200 && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getGroupList(groupName: String?, page: Int, size: Int): Result<PageResult<GroupVO>> = try {
        val res = api.getGroupList(groupName = groupName, pageIndex = page, pageSize = size)
        if (res.code == 200 && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun loadMoreGroups(page: Int, refresh: Boolean): Result<GroupPageVO> = try {
        val res = api.loadMoreGroups(page, refresh)
        if (res.code == 200 && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun addDownloadTask(url: String): Result<String> = try {
        val res = api.addDownloadTask(DownLoadQry(url = url))
        if (res.code == 200) Result.success(res.message ?: "任务已添加")
        else Result.failure(Exception(res.message ?: "添加失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getWorkList(
        page: Int,
        size: Int,
        authorId: String?,
        tagId: Long?,
        keyword: String?,
        seed: Int?
    ): Result<XhsWorkPageVO> = try {
        val res = api.getXhsWorkList(page, size, authorId, tagId, keyword, seed)
        if (res.code == 200 && res.data != null) {
            cacheWorkList(authorId, tagId, keyword, res.data.works)
            Result.success(res.data)
        } else {
            loadCachedWorkList(authorId, tagId, keyword)
                ?.let { Result.success(XhsWorkPageVO(works = it, hasMore = false)) }
                ?: Result.failure(Exception(res.message ?: "加载失败"))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        loadCachedWorkList(authorId, tagId, keyword)
            ?.let { Result.success(XhsWorkPageVO(works = it, hasMore = false)) }
            ?: Result.failure(e)
    }

    suspend fun getWorkDetail(workId: String): Result<XhsWorkDetailVO> = try {
        val res = api.getXhsWorkDetail(workId)
        if (res.code == 200 && res.data != null) Result.success(res.data)
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun deleteWork(workId: String): Result<String> = try {
        val res = api.deleteWork(workId)
        if (res.code == 200) Result.success(res.message ?: "删除成功")
        else Result.failure(Exception(res.message ?: "删除失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun deleteMedia(id: Long): Result<String> = try {
        val res = api.deleteMedia(id)
        if (res.code == 200) Result.success(res.message ?: "删除成功")
        else Result.failure(Exception(res.message ?: "删除失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getAuthors(): Result<List<AuthorVO>> = try {
        val res = api.getAuthors()
        if (res.code == 200) Result.success(res.data ?: emptyList())
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getTags(): Result<List<TagVO>> = try {
        val res = api.getTags()
        if (res.code == 200) Result.success(res.data ?: emptyList())
        else Result.failure(Exception(res.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getRandomGif(): Result<RandomGifVO> = try {
        val res = api.getRandomGif()
        if (res.code == 200 && res.data != null) {
            cache("random_gif", moshi.adapter(RandomGifVO::class.java).toJson(res.data))
            Result.success(res.data)
        } else {
            loadRandomGifFromCache() ?: Result.failure(Exception(res.message ?: "加载失败"))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        loadRandomGifFromCache() ?: Result.failure(e)
    }

    private suspend fun loadRandomGifFromCache(): Result<RandomGifVO>? {
        val payload = cacheDao.findByKey("random_gif")?.payload ?: return null
        val value = moshi.adapter(RandomGifVO::class.java).fromJson(payload) ?: return null
        return Result.success(value)
    }

    suspend fun getCurrentEnvInfo(): Result<PicCount> = try {
        val env = api.getCurrentEnv()
        val info = api.getCurrentEnvInfo()
        if (env.code == 200 && env.data != null) {
            prefs.saveEnv(env.data)
        }
        if (info.code == 200 && info.data != null) Result.success(info.data)
        else Result.failure(Exception(info.message ?: "加载失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun switchEnv(env: String): Result<String> = try {
        val res = api.switchEnv(env)
        if (res.code == 200) {
            prefs.saveEnv(env)
            Result.success(res.message ?: "切换成功")
        } else Result.failure(Exception(res.message ?: "切换失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun getPrivacyMode(): Result<Boolean> = try {
        val res = api.getPrivacyMode()
        if (res.code == 200 && res.data != null) {
            prefs.savePrivacy(res.data)
            Result.success(res.data)
        } else Result.failure(Exception(res.message ?: "读取失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun setPrivacyMode(enabled: Boolean): Result<Boolean> = try {
        val res = api.setPrivacyMode(enabled)
        if (res.code == 200 && res.data != null) {
            prefs.savePrivacy(enabled)
            Result.success(res.data)
        } else Result.failure(Exception(res.message ?: "设置失败"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    suspend fun saveViewMode(mode: String) = prefs.saveViewMode(mode)

    suspend fun saveAutoReadClipboard(enabled: Boolean) = prefs.saveAutoReadClipboard(enabled)

    private suspend fun cacheWorkList(authorId: String?, tagId: Long?, keyword: String?, data: List<XhsWorkListVO>) {
        val key = "work_list_${authorId ?: "all"}_${tagId ?: "all"}_${keyword ?: "all"}"
        val listType = Types.newParameterizedType(List::class.java, XhsWorkListVO::class.java)
        val payload = moshi.adapter<List<XhsWorkListVO>>(listType).toJson(data)
        cache(key, payload)
    }

    private suspend fun loadCachedWorkList(authorId: String?, tagId: Long?, keyword: String?): List<XhsWorkListVO>? {
        val key = "work_list_${authorId ?: "all"}_${tagId ?: "all"}_${keyword ?: "all"}"
        val payload = cacheDao.findByKey(key)?.payload ?: return null
        val listType = Types.newParameterizedType(List::class.java, XhsWorkListVO::class.java)
        return moshi.adapter<List<XhsWorkListVO>>(listType).fromJson(payload)
    }

    private suspend fun cache(key: String, payload: String) {
        cacheDao.upsert(CachedPayload(key = key, payload = payload, updatedAt = System.currentTimeMillis()))
    }
}
