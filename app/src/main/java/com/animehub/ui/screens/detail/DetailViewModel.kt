package com.animehub.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.domain.model.AnimeDetail
import com.animehub.domain.model.Episode
import com.animehub.domain.model.WatchHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val detail: AnimeDetail,
        val isFavorite: Boolean,
        val watchProgress: Map<String, WatchHistory> = emptyMap()
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var animeId: String = ""
    private var sourceId: String = ""

    fun load(animeId: String, sourceId: String) {
        this.animeId = animeId
        this.sourceId = sourceId
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val detail = app.animeRepository.getDetail(animeId, sourceId)
                val isFav = app.favoriteRepository.isFavorite(animeId, sourceId)
                _uiState.value = DetailUiState.Success(detail = detail, isFavorite = isFav)
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(
                    e.message ?: "加载详情失败"
                )
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            app.favoriteRepository.toggleFavorite(animeId, sourceId)
            val isFav = app.favoriteRepository.isFavorite(animeId, sourceId)
            val current = _uiState.value
            if (current is DetailUiState.Success) {
                _uiState.value = current.copy(isFavorite = isFav)
            }
        }
    }
}
