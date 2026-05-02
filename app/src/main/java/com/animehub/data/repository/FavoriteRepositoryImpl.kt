package com.animehub.data.repository

import com.animehub.data.local.dao.AnimeDao
import com.animehub.data.local.dao.FavoriteDao
import com.animehub.data.local.entity.FavoriteEntity
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val animeDao: AnimeDao
) : FavoriteRepository {

    override fun getAllFavorites(): Flow<List<AnimeSummary>> {
        return favoriteDao.getAllFlow().map { favorites ->
            val animeIds = favorites.map { it.animeId }
            if (animeIds.isEmpty()) return@map emptyList()
            val animeMap = animeDao.getByIds(animeIds).associateBy { it.id }
            favorites.mapNotNull { fav ->
                animeMap[fav.animeId]?.let { entity ->
                    AnimeSummary(entity.id, entity.sourceId, entity.title, entity.coverUrl)
                }
            }
        }
    }

    override suspend fun isFavorite(animeId: String, sourceId: String): Boolean {
        return favoriteDao.getById(animeId, sourceId) != null
    }

    override suspend fun toggleFavorite(animeId: String, sourceId: String) {
        val existing = favoriteDao.getById(animeId, sourceId)
        if (existing != null) {
            favoriteDao.delete(animeId, sourceId)
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    animeId = animeId,
                    sourceId = sourceId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
