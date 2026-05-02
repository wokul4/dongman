package com.animehub.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.domain.model.WatchHistory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PlayerUiState {
    data object Loading : PlayerUiState()
    data class Ready(val media: PlayableMediaState, val initialProgressMs: Long = 0L) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

data class PlayableMediaState(val uri: String)
data class PlaybackState(
    val episodeId: String = "",
    val animeId: String = "",
    val sourceId: String = "",
    val progressMs: Long = 0L,
    val durationMs: Long = 0L
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var saveJob: Job? = null

    fun load(episodeId: String, animeId: String, sourceId: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val playable = app.animeRepository.getPlayable(episodeId, sourceId)

                _playbackState.value = PlaybackState(
                    episodeId = episodeId,
                    animeId = animeId,
                    sourceId = sourceId
                )

                val progress = app.watchHistoryRepository.getProgress(episodeId, animeId, sourceId)
                _uiState.value = PlayerUiState.Ready(
                    media = PlayableMediaState(playable.uri),
                    initialProgressMs = progress?.progressMs ?: 0L
                )
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(
                    e.message ?: "加载播放地址失败"
                )
            }
        }
    }

    fun onProgressChanged(progressMs: Long, durationMs: Long) {
        _playbackState.value = _playbackState.value.copy(
            progressMs = progressMs,
            durationMs = durationMs
        )
    }

    fun startAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                saveCurrentProgress()
            }
        }
    }

    fun stopAutoSave() {
        saveJob?.cancel()
        saveJob = null
    }

    fun saveCurrentProgress() {
        val state = _playbackState.value
        if (state.episodeId.isEmpty() || state.animeId.isEmpty()) return
        viewModelScope.launch {
            app.watchHistoryRepository.saveProgress(
                WatchHistory(
                    episodeId = state.episodeId,
                    animeId = state.animeId,
                    sourceId = state.sourceId,
                    progressMs = state.progressMs,
                    durationMs = state.durationMs,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgress()
    }
}
