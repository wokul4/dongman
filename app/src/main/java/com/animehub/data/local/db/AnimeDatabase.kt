package com.animehub.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.animehub.data.local.dao.AnimeDao
import com.animehub.data.local.dao.EpisodeDao
import com.animehub.data.local.dao.FavoriteDao
import com.animehub.data.local.dao.SourceDao
import com.animehub.data.local.dao.WatchHistoryDao
import com.animehub.data.local.entity.AnimeEntity
import com.animehub.data.local.entity.EpisodeEntity
import com.animehub.data.local.entity.FavoriteEntity
import com.animehub.data.local.entity.SourceEntity
import com.animehub.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        SourceEntity::class,
        AnimeEntity::class,
        EpisodeEntity::class,
        WatchHistoryEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AnimeDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun animeDao(): AnimeDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDatabase? = null

        fun getInstance(context: Context): AnimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDatabase::class.java,
                    "animehub.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
