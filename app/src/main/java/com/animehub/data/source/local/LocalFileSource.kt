package com.animehub.data.source.local

import android.content.Context
import android.net.Uri
import com.animehub.data.local.dao.AnimeDao
import com.animehub.data.local.dao.EpisodeDao
import com.animehub.data.local.entity.AnimeEntity
import com.animehub.data.local.entity.EpisodeEntity
import com.animehub.domain.model.AnimeDetail
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.model.Episode
import com.animehub.domain.model.PageResult
import com.animehub.domain.model.PlayableMedia
import com.animehub.domain.source.AnimeSource
import com.animehub.domain.source.SourceType

class LocalFileSource(
    private val context: Context,
    private val animeDao: AnimeDao,
    private val episodeDao: EpisodeDao
) : AnimeSource {

    override val id: String = "local_file"
    override val name: String = "本地视频"
    override val type: SourceType = SourceType.LOCAL_FILE

    val importManager = LocalImportManager(context, animeDao, episodeDao)

    override suspend fun search(query: String, page: Int): PageResult<AnimeSummary> {
        val results = animeDao.searchByTitle(query)
            .filter { it.sourceId == id }
        return PageResult(
            items = results.map { it.toSummary() },
            page = 1,
            totalPages = 1,
            hasMore = false
        )
    }

    override suspend fun getDetail(animeId: String): AnimeDetail {
        val anime = animeDao.getById(animeId, id)
            ?: throw IllegalStateException("Anime not found: $animeId")
        val episodes = episodeDao.getByAnimeId(animeId, id)
        return anime.toDetail(episodes.map { it.toDomain() })
    }

    override suspend fun getEpisodes(animeId: String): List<Episode> {
        return episodeDao.getByAnimeId(animeId, id).map { it.toDomain() }
    }

    override suspend fun getPlayable(episodeId: String): PlayableMedia {
        return PlayableMedia(uri = episodeId)
    }

    private fun AnimeEntity.toSummary() = AnimeSummary(id, sourceId, title, coverUrl)
    private fun AnimeEntity.toDetail(episodes: List<Episode>) = AnimeDetail(id, sourceId, title, coverUrl, description, episodes)
    private fun EpisodeEntity.toDomain() = Episode(id, animeId, sourceId, title, episodeNumber, durationMs, playableUrl)
}
