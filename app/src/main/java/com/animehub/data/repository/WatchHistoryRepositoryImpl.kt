package com.animehub.data.repository

import com.animehub.data.local.dao.WatchHistoryDao
import com.animehub.data.local.entity.WatchHistoryEntity
import com.animehub.domain.model.WatchHistory
import com.animehub.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchHistoryRepositoryImpl(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {

    override fun getRecentHistory(): Flow<List<WatchHistory>> {
        return watchHistoryDao.getRecentFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProgress(episodeId: String, animeId: String, sourceId: String): WatchHistory? {
        return watchHistoryDao.getById(episodeId, animeId, sourceId)?.toDomain()
    }

    override suspend fun saveProgress(history: WatchHistory) {
        watchHistoryDao.insert(history.toEntity())
    }

    private fun WatchHistoryEntity.toDomain() = WatchHistory(
        episodeId = episodeId,
        animeId = animeId,
        sourceId = sourceId,
        progressMs = progressMs,
        durationMs = durationMs,
        lastWatchedAt = lastWatchedAt
    )

    private fun WatchHistory.toEntity() = WatchHistoryEntity(
        episodeId = episodeId,
        animeId = animeId,
        sourceId = sourceId,
        progressMs = progressMs,
        durationMs = durationMs,
        lastWatchedAt = lastWatchedAt
    )
}
