package com.animehub.domain.repository

import com.animehub.domain.model.SourceConfig
import kotlinx.coroutines.flow.Flow

interface SourceRepository {
    fun getAllSources(): Flow<List<SourceConfig>>
    suspend fun addSource(config: SourceConfig)
    suspend fun deleteSource(id: String)
    suspend fun setEnabled(id: String, enabled: Boolean)
}
