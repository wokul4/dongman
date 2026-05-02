package com.animehub.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.domain.model.AnimeSummary
import com.animehub.domain.model.SourceConfig
import com.animehub.domain.model.WatchHistory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RecentWatchItem(
    val history: WatchHistory,
    val episodeTitle: String?,
    val animeTitle: String?
)

data class HomeUiState(
    val recentHistory: List<RecentWatchItem> = emptyList(),
    val favorites: List<AnimeSummary> = emptyList(),
    val enabledSources: List<SourceConfig> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.watchHistoryRepository.getRecentHistory().collect { history ->
                val enriched = history.map { h ->
                    val episode = try {
                        app.database.episodeDao().getById(h.episodeId, h.animeId, h.sourceId)
                    } catch (_: Exception) { null }
                    val anime = try {
                        app.database.animeDao().getById(h.animeId, h.sourceId)
                    } catch (_: Exception) { null }
                    RecentWatchItem(
                        history = h,
                        episodeTitle = episode?.title,
                        animeTitle = anime?.title
                    )
                }
                _uiState.update { it.copy(recentHistory = enriched) }
            }
        }
        viewModelScope.launch {
            app.favoriteRepository.getAllFavorites().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
        viewModelScope.launch {
            app.sourceRepository.getAllSources().collect { sources ->
                _uiState.update {
                    it.copy(
                        enabledSources = sources,
                        isLoading = false
                    )
                }
            }
        }
    }
}
