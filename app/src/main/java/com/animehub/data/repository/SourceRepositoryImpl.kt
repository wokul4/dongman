package com.animehub.data.repository

import com.animehub.data.local.entity.SourceEntity
import com.animehub.data.source.SourceManager
import com.animehub.domain.model.SourceConfig
import com.animehub.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json.Default

class SourceRepositoryImpl(
    private val sourceManager: SourceManager
) : SourceRepository {

    override fun getAllSources(): Flow<List<SourceConfig>> {
        return sourceManager.allSourcesFlow.map { sources ->
            sources.map { SourceConfig(it.id, it.name, it.type.name) }
        }
    }

    override suspend fun addSource(config: SourceConfig) {
        sourceManager.addSource(
            SourceEntity(
                id = config.id,
                name = config.name,
                type = config.type,
                configJson = json.encodeToString(config.config),
                enabled = config.enabled,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteSource(id: String) {
        sourceManager.removeSource(id)
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        sourceManager.setEnabled(id, enabled)
    }
}
