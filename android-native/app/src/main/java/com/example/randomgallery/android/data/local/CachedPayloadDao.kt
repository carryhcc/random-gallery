package com.example.randomgallery.android.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CachedPayloadDao {

    @Upsert
    suspend fun upsert(item: CachedPayload)

    @Query("SELECT * FROM cached_payload WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): CachedPayload?

    @Query("DELETE FROM cached_payload")
    suspend fun clearAll()
}
