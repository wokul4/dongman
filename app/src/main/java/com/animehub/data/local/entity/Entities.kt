package com.animehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val configJson: String,
    val enabled: Boolean,
    val createdAt: Long
)

@Entity(tableName = "anime", primaryKeys = ["id", "sourceId"])
data class AnimeEntity(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val description: String?,
    val updatedAt: Long
)

@Entity(tableName = "episodes", primaryKeys = ["id", "animeId", "sourceId"])
data class EpisodeEntity(
    val id: String,
    val animeId: String,
    val sourceId: String,
    val title: String,
    val episodeNumber: Float?,
    val durationMs: Long?,
    val playableUrl: String?
)

@Entity(tableName = "watch_history", primaryKeys = ["episodeId", "animeId", "sourceId"])
data class WatchHistoryEntity(
    val episodeId: String,
    val animeId: String,
    val sourceId: String,
    val progressMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long
)

@Entity(tableName = "favorites", primaryKeys = ["animeId", "sourceId"])
data class FavoriteEntity(
    val animeId: String,
    val sourceId: String,
    val createdAt: Long
)
