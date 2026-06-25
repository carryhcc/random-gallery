package com.example.randomgallery.android.data.local

import android.content.Context
import androidx.room.Room

object DatabaseModule {

    @Volatile
    private var database: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "random_gallery_native.db"
            ).build().also { database = it }
        }
    }
}
