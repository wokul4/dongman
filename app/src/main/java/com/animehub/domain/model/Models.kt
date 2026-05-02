package com.animehub.domain.model

data class AnimeSummary(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String? = null
)

data class AnimeDetail(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: String,
    val animeId: String,
    val sourceId: String,
    val title: String,
    val episodeNumber: Float? = null,
    val durationMs: Long? = null,
    val playableUrl: String? = null
)

data class PlayableMedia(
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

data class SubtitleTrack(
    val uri: String,
    val label: String,
    val language: String? = null
)

data class WatchHistory(
    val episodeId: String,
    val animeId: String,
    val sourceId: String,
    val progressMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long
)

data class FavoriteAnime(
    val animeId: String,
    val sourceId: String,
    val createdAt: Long
)

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int = 1,
    val hasMore: Boolean = false
)

data class SourceConfig(
    val id: String,
    val name: String,
    val type: String,
    val enabled: Boolean = true,
    val config: Map<String, String> = emptyMap()
)
