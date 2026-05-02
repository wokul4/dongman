package com.animehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.animehub.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE animeId = :animeId AND sourceId = :sourceId ORDER BY episodeNumber ASC")
    fun getByAnimeIdFlow(animeId: String, sourceId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE animeId = :animeId AND sourceId = :sourceId ORDER BY episodeNumber ASC")
    suspend fun getByAnimeId(animeId: String, sourceId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id AND animeId = :animeId AND sourceId = :sourceId")
    suspend fun getById(id: String, animeId: String, sourceId: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)
}
