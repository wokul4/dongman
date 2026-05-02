package com.animehub.domain.repository

import com.animehub.domain.model.WatchHistory
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getRecentHistory(): Flow<List<WatchHistory>>
    suspend fun getProgress(episodeId: String, animeId: String, sourceId: String): WatchHistory?
    suspend fun saveProgress(history: WatchHistory)
}
