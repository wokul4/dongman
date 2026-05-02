package com.animehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.animehub.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE episodeId = :episodeId AND animeId = :animeId AND sourceId = :sourceId")
    suspend fun getById(episodeId: String, animeId: String, sourceId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE episodeId = :episodeId AND animeId = :animeId AND sourceId = :sourceId")
    suspend fun delete(episodeId: String, animeId: String, sourceId: String)
}
