package com.animehub.domain.source

import com.animehub.domain.model.AnimeDetail
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.model.Episode
import com.animehub.domain.model.PageResult
import com.animehub.domain.model.PlayableMedia

interface AnimeSource {
    val id: String
    val name: String
    val type: SourceType

    suspend fun search(query: String, page: Int = 1): PageResult<AnimeSummary>
    suspend fun getDetail(animeId: String): AnimeDetail
    suspend fun getEpisodes(animeId: String): List<Episode>
    suspend fun getPlayable(episodeId: String): PlayableMedia
}
