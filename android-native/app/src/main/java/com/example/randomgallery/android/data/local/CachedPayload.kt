package com.example.randomgallery.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_payload")
data class CachedPayload(
    @PrimaryKey val key: String,
    val payload: String,
    val updatedAt: Long
)
