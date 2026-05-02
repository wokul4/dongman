package com.animehub.data.source

import android.content.Context
import com.animehub.data.local.dao.AnimeDao
import com.animehub.data.local.dao.EpisodeDao
import com.animehub.data.local.dao.SourceDao
import com.animehub.data.local.entity.SourceEntity
import com.animehub.data.source.local.LocalFileSource
import com.animehub.data.source.webdav.WebDavConfig
import com.animehub.data.source.webdav.WebDavSource
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.model.PageResult
import com.animehub.domain.source.AnimeSource
import com.animehub.domain.source.SourceType
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourceManager(
    private val context: Context,
    private val sourceDao: SourceDao,
    private val animeDao: AnimeDao,
    private val episodeDao: EpisodeDao
) {
    private val sources = mutableMapOf<String, AnimeSource>()

    val allSourcesFlow: Flow<List<AnimeSource>> = sourceDao.getAllFlow().map { entities ->
        entities.mapNotNull { entity -> getOrCreateSource(entity) }
    }

    val enabledSourcesFlow: Flow<List<AnimeSource>> = sourceDao.getAllFlow().map { entities ->
        entities.filter { it.enabled }
            .mapNotNull { entity -> getOrCreateSource(entity) }
    }

    suspend fun loadSources() {
        val entities = sourceDao.getAllEnabled()
        entities.forEach { entity -> getOrCreateSource(entity) }
    }

    suspend fun addSource(entity: SourceEntity) {
        sourceDao.insert(entity)
        getOrCreateSource(entity)
    }

    suspend fun removeSource(id: String) {
        sourceDao.deleteById(id)
        sources.remove(id)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        sourceDao.setEnabled(id, enabled)
        if (!enabled) sources.remove(id)
    }

    suspend fun getAllSources(): List<AnimeSource> {
        return sourceDao.getAllEnabled().mapNotNull { getOrCreateSource(it) }
    }

    suspend fun getSourcesByType(type: SourceType): List<AnimeSource> {
        return getAllSources().filter { it.type == type }
    }

    fun getSource(id: String): AnimeSource? = sources[id]

    suspend fun searchAll(query: String, page: Int = 1): List<Pair<AnimeSource, PageResult<AnimeSummary>>> {
        val activeSources = sourceDao.getAllEnabled()
        val results = mutableListOf<Pair<AnimeSource, PageResult<AnimeSummary>>>()
        for (entity in activeSources) {
            try {
                val source = getOrCreateSource(entity) ?: continue
                val result = source.search(query, page)
                if (result.items.isNotEmpty()) {
                    results.add(source to result)
                }
            } catch (_: Exception) {
                // source failure shouldn't affect others
            }
        }
        return results
    }

    fun getLocalSource(): LocalFileSource? {
        return sources["local_file"] as? LocalFileSource
    }

    fun getWebDavSource(id: String): WebDavSource? {
        return sources[id] as? WebDavSource
    }

    fun getOrCreateWebDavSource(id: String, config: WebDavConfig): WebDavSource {
        val existing = sources[id] as? WebDavSource
        if (existing != null) return existing
        val source = WebDavSource(id, config.displayName, config, animeDao, episodeDao)
        sources[id] = source
        return source
    }

    private fun getOrCreateSource(entity: SourceEntity): AnimeSource? {
        if (sources.containsKey(entity.id)) return sources[entity.id]
        val source = when (entity.type) {
            SourceType.LOCAL_FILE.name -> LocalFileSource(context, animeDao, episodeDao)
            SourceType.WEBDAV.name -> {
                try {
                    val config = Json.decodeFromString<WebDavConfig>(entity.configJson)
                    WebDavSource(entity.id, config.displayName, config, animeDao, episodeDao)
                } catch (_: Exception) { null }
            }
            else -> null
        }
        if (source != null) sources[entity.id] = source
        return source
    }
}
