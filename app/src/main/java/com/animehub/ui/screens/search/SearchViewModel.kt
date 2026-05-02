package com.animehub.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.domain.model.AnimeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: List<AnimeSummary>) : SearchUiState()
    data object Empty : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = app.animeRepository.searchAllSources(q)
                _uiState.value = if (results.isEmpty()) {
                    SearchUiState.Empty
                } else {
                    SearchUiState.Success(results)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    e.message ?: "搜索出错"
                )
            }
        }
    }
}
