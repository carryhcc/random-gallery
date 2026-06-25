package com.example.randomgallery.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedPayloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CachedPayload)

    @Query("SELECT * FROM cached_payload WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): CachedPayload?
}
