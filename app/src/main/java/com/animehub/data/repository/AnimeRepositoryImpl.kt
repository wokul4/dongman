package com.animehub.data.repository

import com.animehub.data.source.SourceManager
import com.animehub.domain.model.AnimeDetail
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.model.Episode
import com.animehub.domain.model.PlayableMedia
import com.animehub.domain.repository.AnimeRepository

class AnimeRepositoryImpl(
    private val sourceManager: SourceManager
) : AnimeRepository {

    override suspend fun searchAllSources(query: String): List<AnimeSummary> {
        val results = sourceManager.searchAll(query)
        return results.flatMap { (_, pageResult) -> pageResult.items }
    }

    override suspend fun getDetail(animeId: String, sourceId: String): AnimeDetail {
        val source = sourceManager.getSource(sourceId)
            ?: throw IllegalStateException("Source not found: $sourceId")
        return source.getDetail(animeId)
    }

    override suspend fun getEpisodes(animeId: String, sourceId: String): List<Episode> {
        val source = sourceManager.getSource(sourceId)
            ?: throw IllegalStateException("Source not found: $sourceId")
        return source.getEpisodes(animeId)
    }

    override suspend fun getPlayable(episodeId: String, sourceId: String): PlayableMedia {
        val source = sourceManager.getSource(sourceId)
            ?: throw IllegalStateException("Source not found: $sourceId")
        return source.getPlayable(episodeId)
    }
}
