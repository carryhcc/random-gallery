package com.example.randomgallery.android.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
    val timestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class PicVO(
    val id: Long? = null,
    val groupId: Long? = null,
    val picName: String? = null,
    val picUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class PicQry(
    val groupId: Long? = null,
    val pageSize: Int = 10,
    val pageIndex: Int = 1
)

@JsonClass(generateAdapter = true)
data class GroupVO(
    val groupId: Long? = null,
    val groupName: String? = null,
    val groupUrl: String? = null,
    val groupCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class GroupQry(
    val groupName: String? = null,
    val groupId: Long? = null,
    val groupIdList: List<Long>? = null,
    val pageSize: Int = 10,
    val pageIndex: Int = 1
)

@JsonClass(generateAdapter = true)
data class PageResult<T>(
    val list: List<T> = emptyList(),
    val total: Long = 0,
    val pageNum: Long = 1,
    val pageSize: Long = 10,
    val pages: Int = 1,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GroupPageVO(
    val images: List<GroupVO> = emptyList(),
    val hasMore: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DownLoadQry(
    val url: String,
    val download: Boolean = true,
    val index: List<Int>? = null,
    val cookie: String? = null,
    val proxy: String? = null,
    val skip: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class XhsWorkListVO(
    val id: Long? = null,
    val workId: String? = null,
    val workTitle: String? = null,
    val authorNickname: String? = null,
    val publishTime: String? = null,
    val coverImageUrl: String? = null,
    val imageCount: Int? = null,
    val gifCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class XhsWorkPageVO(
    val works: List<XhsWorkListVO> = emptyList(),
    val hasMore: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AuthorVO(
    val authorId: String? = null,
    val authorNickname: String? = null,
    val authorUrl: String? = null,
    val workCount: Long? = null
)

@JsonClass(generateAdapter = true)
data class TagVO(
    val id: Long? = null,
    val tagName: String? = null,
    val workCount: Long? = null
)

@JsonClass(generateAdapter = true)
data class XhsWorkBase(
    val id: Long? = null,
    val workId: String? = null,
    val workUrl: String? = null,
    val workTitle: String? = null,
    val workDescription: String? = null,
    val publishTime: String? = null,
    val authorNickname: String? = null,
    val authorId: String? = null,
    val authorUrl: String? = null,
    val likeCount: String? = null,
    val collectCount: String? = null,
    val commentCount: String? = null,
    val shareCount: String? = null,
    val workTags: String? = null
)

@JsonClass(generateAdapter = true)
data class XhsWorkMedia(
    val id: Long? = null,
    val workBaseId: Long? = null,
    val workId: String? = null,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val sortIndex: Int? = null
)

@JsonClass(generateAdapter = true)
data class XhsWorkDetailVO(
    val baseInfo: XhsWorkBase? = null,
    val images: List<XhsWorkMedia> = emptyList(),
    val gifs: List<XhsWorkMedia> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RandomGifVO(
    val id: Long? = null,
    val mediaUrl: String? = null,
    val workId: String? = null,
    val workBaseId: Long? = null,
    val workTitle: String? = null,
    val authorNickname: String? = null,
    val authorId: String? = null
)

@JsonClass(generateAdapter = true)
data class PicCount(
    val env: String? = null,
    val picCount: Long? = null,
    val groupCount: Long? = null
)
