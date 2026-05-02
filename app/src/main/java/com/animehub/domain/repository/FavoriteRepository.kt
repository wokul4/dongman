package com.animehub.domain.repository

import com.animehub.domain.model.AnimeSummary
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getAllFavorites(): Flow<List<AnimeSummary>>
    suspend fun isFavorite(animeId: String, sourceId: String): Boolean
    suspend fun toggleFavorite(animeId: String, sourceId: String)
}
