package com.animehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.animehub.data.local.entity.AnimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime WHERE title LIKE '%' || :query || '%'")
    suspend fun searchByTitle(query: String): List<AnimeEntity>

    @Query("SELECT * FROM anime WHERE sourceId = :sourceId")
    fun getBySourceIdFlow(sourceId: String): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE id = :id AND sourceId = :sourceId")
    suspend fun getById(id: String, sourceId: String): AnimeEntity?

    @Query("SELECT * FROM anime WHERE id IN (SELECT animeId FROM favorites)")
    fun getFavoritesFlow(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime")
    suspend fun getAll(): List<AnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(anime: List<AnimeEntity>)

    @Query("SELECT * FROM anime WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<AnimeEntity>
}
