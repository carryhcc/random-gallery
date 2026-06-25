package com.example.randomgallery.android.data.network

import com.example.randomgallery.android.data.model.*
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/pic/random/one")
    suspend fun getRandomPic(): ApiResponse<PicVO>

    @GET("api/pic/list")
    suspend fun getPicList(
        @Query("groupId") groupId: Long?,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: Int
    ): ApiResponse<List<PicVO>>

    @GET("api/group/randomGroupInfo")
    suspend fun getRandomGroupInfo(@Query("groupId") groupId: Long? = null): ApiResponse<GroupVO>

    @GET("api/group/list")
    suspend fun getGroupList(
        @Query("groupName") groupName: String?,
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: Int
    ): ApiResponse<PageResult<GroupVO>>

    @GET("api/group/loadMore")
    suspend fun loadMoreGroups(
        @Query("page") page: Int = 0,
        @Query("refresh") refresh: Boolean = false
    ): ApiResponse<GroupPageVO>

    @POST("api/xhsWork/download")
    suspend fun addDownloadTask(@Body qry: DownLoadQry): ApiResponse<String>

    @GET("api/xhsWork/list")
    suspend fun getXhsWorkList(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("authorId") authorId: String? = null,
        @Query("tagId") tagId: Long? = null,
        @Query("str") keyword: String? = null,
        @Query("seed") seed: Int? = null
    ): ApiResponse<XhsWorkPageVO>

    @GET("api/xhsWork/detail/{workId}")
    suspend fun getXhsWorkDetail(@Path("workId") workId: String): ApiResponse<XhsWorkDetailVO>

    @DELETE("api/xhsWork/{workId}")
    suspend fun deleteWork(@Path("workId") workId: String): ApiResponse<String>

    @DELETE("api/xhsWork/media/{id}")
    suspend fun deleteMedia(@Path("id") id: Long): ApiResponse<String>

    @GET("api/xhsWork/authors")
    suspend fun getAuthors(): ApiResponse<List<AuthorVO>>

    @GET("api/xhsWork/tags")
    suspend fun getTags(@Query("limit") limit: Int = 200): ApiResponse<List<TagVO>>

    @GET("api/xhsWork/randomGif")
    suspend fun getRandomGif(): ApiResponse<RandomGifVO>

    @GET("api/system/privacy-mode")
    suspend fun getPrivacyMode(): ApiResponse<Boolean>

    @GET("api/system/privacy-mode")
    suspend fun setPrivacyMode(@Query("enabled") enabled: Boolean): ApiResponse<Boolean>

    @GET("api/system/env/current")
    suspend fun getCurrentEnv(): ApiResponse<String>

    @GET("api/system/env/currentInfo")
    suspend fun getCurrentEnvInfo(): ApiResponse<PicCount>

    @GET("api/system/env/{env}")
    suspend fun switchEnv(@Path("env") env: String): ApiResponse<String>
}
