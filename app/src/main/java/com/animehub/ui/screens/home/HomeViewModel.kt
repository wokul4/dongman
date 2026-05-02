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

data class HomeUiState(
    val recentHistory: List<WatchHistory> = emptyList(),
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
                _uiState.update { it.copy(recentHistory = history) }
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
