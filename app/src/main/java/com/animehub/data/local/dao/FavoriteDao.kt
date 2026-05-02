package com.animehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.animehub.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE animeId = :animeId AND sourceId = :sourceId")
    suspend fun getById(animeId: String, sourceId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE animeId = :animeId AND sourceId = :sourceId")
    suspend fun delete(animeId: String, sourceId: String)
}
