package com.animehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.animehub.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE enabled = 1")
    suspend fun getAllEnabled(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: SourceEntity)

    @Query("UPDATE sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteById(id: String)
}
