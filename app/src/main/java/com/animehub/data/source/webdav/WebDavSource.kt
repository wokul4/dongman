package com.animehub.data.source.webdav

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
import com.animehub.util.FileNameParser
import com.animehub.util.VideoFileDetector

class WebDavSource(
    override val id: String,
    override val name: String,
    val config: WebDavConfig,
    private val animeDao: AnimeDao,
    private val episodeDao: EpisodeDao
) : AnimeSource {

    override val type: SourceType = SourceType.WEBDAV
    val client = WebDavClient(config)

    suspend fun importDirectoryAsAnime(dirPath: String, dirName: String): Int {
        val result = client.listDirectory(dirPath)
        val items = when (result) {
            is WebDavClient.WebDavResult.Success -> result.data
            is WebDavClient.WebDavResult.Error -> return 0
        }

        val videoFiles = items
            .filter { !it.isDirectory && VideoFileDetector.isVideoFile(it.name) }
            .sortedBy { it.name }

        if (videoFiles.isEmpty()) return 0

        val now = System.currentTimeMillis()
        val animeId = "webdav_${id}_$dirPath".replace(Regex("[/:?&=#]"), "_")

        val animeEntity = AnimeEntity(
            id = animeId,
            sourceId = id,
            title = dirName,
            coverUrl = null,
            description = "来自 WebDAV: $dirPath",
            updatedAt = now
        )

        val episodeEntities = videoFiles.mapIndexed { index, item ->
            val episodeNum = FileNameParser.parseEpisodeNumber(item.name) ?: (index + 1).toFloat()
            EpisodeEntity(
                id = "webdav_${id}_${item.path}".replace(Regex("[/:?&=# ]"), "_"),
                animeId = animeId,
                sourceId = id,
                title = item.name,
                episodeNumber = episodeNum,
                durationMs = null,
                playableUrl = item.path
            )
        }

        animeDao.insertAll(listOf(animeEntity))
        episodeDao.insertAll(episodeEntities)
        return episodeEntities.size
    }

    suspend fun importSingleVideo(item: WebDavItem, animeTitle: String): Int {
        val now = System.currentTimeMillis()
        val animeId = "webdav_${id}_single_${item.path}".replace(Regex("[/:?&=#]"), "_")
        val episodeId = "webdav_${id}_${item.path}".replace(Regex("[/:?&=# ]"), "_")

        val animeEntity = AnimeEntity(
            id = animeId,
            sourceId = id,
            title = animeTitle,
            coverUrl = null,
            description = null,
            updatedAt = now
        )
        val episodeEntity = EpisodeEntity(
            id = episodeId,
            animeId = animeId,
            sourceId = id,
            title = item.name,
            episodeNumber = FileNameParser.parseEpisodeNumber(item.name) ?: 1f,
            durationMs = null,
            playableUrl = item.path
        )

        animeDao.insertAll(listOf(animeEntity))
        episodeDao.insertAll(listOf(episodeEntity))
        return 1
    }

    override suspend fun search(query: String, page: Int): PageResult<AnimeSummary> {
        val results = animeDao.searchByTitle(query).filter { it.sourceId == id }
        return PageResult(
            items = results.map { AnimeSummary(it.id, it.sourceId, it.title, it.coverUrl) },
            page = 1, totalPages = 1, hasMore = false
        )
    }

    override suspend fun getDetail(animeId: String): AnimeDetail {
        val anime = animeDao.getById(animeId, id)
            ?: throw IllegalStateException("Anime not found: $animeId")
        val episodes = episodeDao.getByAnimeId(animeId, id)
        return AnimeDetail(
            id = anime.id, sourceId = anime.sourceId,
            title = anime.title, coverUrl = anime.coverUrl,
            description = anime.description,
            episodes = episodes.map { Episode(it.id, it.animeId, it.sourceId, it.title, it.episodeNumber, it.durationMs, it.playableUrl) }
        )
    }

    override suspend fun getEpisodes(animeId: String): List<Episode> {
        return episodeDao.getByAnimeId(animeId, id).map {
            Episode(it.id, it.animeId, it.sourceId, it.title, it.episodeNumber, it.durationMs, it.playableUrl)
        }
    }

    override suspend fun getPlayable(episodeId: String): PlayableMedia {
        val animeId = animeDao.getAll().filter { it.sourceId == id }.firstOrNull()?.id ?: ""
        val episode = episodeDao.getById(episodeId, animeId, id)
        val path = episode?.playableUrl ?: throw IllegalStateException("Episode not found: $episodeId")
        val url = client.buildPlayableUrl(path)
        val headers = client.buildAuthHeader()
        return PlayableMedia(uri = url, headers = headers)
    }
}
