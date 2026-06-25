package com.example.randomgallery.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CachedPayload::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CachedPayloadDao
}
