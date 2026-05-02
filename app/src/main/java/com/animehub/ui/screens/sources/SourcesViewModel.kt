package com.animehub.ui.screens.sources

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.data.local.entity.SourceEntity
import com.animehub.domain.model.SourceConfig
import com.animehub.domain.source.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourcesUiState(
    val sources: List<SourceConfig> = emptyList(),
    val importResult: String? = null
)

class SourcesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.sourceRepository.getAllSources().collect { sources ->
                _uiState.update { it.copy(sources = sources) }
            }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            app.sourceRepository.setEnabled(id, enabled)
        }
    }

    fun deleteSource(id: String) {
        viewModelScope.launch {
            app.sourceRepository.deleteSource(id)
        }
    }

    fun importLocalVideos(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                ensureLocalSource()
                val localSource = app.sourceManager.getLocalSource() ?: run {
                    _uiState.update { it.copy(importResult = "本地源不可用") }
                    return@launch
                }
                val result = localSource.importManager.importMultipleVideos(uris)
                val msg = "成功导入 ${result.animeCount} 个番剧，${result.episodeCount} 个剧集" +
                        if (result.failedCount > 0) "，${result.failedCount} 个失败" else ""
                _uiState.update { it.copy(importResult = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(importResult = "导入失败: ${e.message}") }
            }
        }
    }

    fun importLocalSingleVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                ensureLocalSource()
                val localSource = app.sourceManager.getLocalSource() ?: run {
                    _uiState.update { it.copy(importResult = "本地源不可用") }
                    return@launch
                }
                val result = localSource.importManager.importSingleVideo(uri)
                _uiState.update {
                    it.copy(importResult = "成功导入 ${result.episodeCount} 个视频")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importResult = "导入失败: ${e.message}") }
            }
        }
    }

    fun importLocalTreeUri(treeUri: Uri, animeTitle: String) {
        viewModelScope.launch {
            try {
                ensureLocalSource()
                val localSource = app.sourceManager.getLocalSource() ?: run {
                    _uiState.update { it.copy(importResult = "本地源不可用") }
                    return@launch
                }
                val result = localSource.importManager.importFromTreeUri(treeUri, animeTitle)
                val msg = "成功导入 ${result.animeCount} 个番剧，${result.episodeCount} 个剧集"
                _uiState.update { it.copy(importResult = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(importResult = "导入失败: ${e.message}") }
            }
        }
    }

    private suspend fun ensureLocalSource() {
        if (app.sourceManager.getLocalSource() == null) {
            app.sourceManager.addSource(
                SourceEntity(
                    id = "local_file",
                    name = "本地视频",
                    type = SourceType.LOCAL_FILE.name,
                    configJson = "{}",
                    enabled = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }
}
