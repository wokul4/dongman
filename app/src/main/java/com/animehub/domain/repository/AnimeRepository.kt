package com.animehub.domain.repository

import com.animehub.domain.model.AnimeDetail
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.model.Episode
import com.animehub.domain.model.PlayableMedia

interface AnimeRepository {
    suspend fun searchAllSources(query: String): List<AnimeSummary>
    suspend fun getDetail(animeId: String, sourceId: String): AnimeDetail
    suspend fun getEpisodes(animeId: String, sourceId: String): List<Episode>
    suspend fun getPlayable(episodeId: String, sourceId: String): PlayableMedia
}
