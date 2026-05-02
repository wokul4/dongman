package com.animehub.ui.screens.sources

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.data.local.entity.SourceEntity
import com.animehub.data.source.webdav.WebDavClient
import com.animehub.data.source.webdav.WebDavConfig
import com.animehub.domain.model.SourceConfig
import com.animehub.domain.source.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
            app.sourceManager.removeSource(id)
        }
    }

    fun testWebDavConnection(url: String, username: String, password: String, rootPath: String): String {
        val config = WebDavConfig(
            baseUrl = url.trimEnd('/'),
            username = username.ifBlank { null },
            password = password.ifBlank { null },
            displayName = "test",
            rootPath = rootPath.ifBlank { "/" }
        )
        val client = WebDavClient(config)
        return when (val result = client.testConnection()) {
            is WebDavClient.WebDavResult.Success -> "连接成功"
            is WebDavClient.WebDavResult.Error -> "连接失败: ${result.message}"
        }
    }

    fun saveWebDavSource(name: String, url: String, username: String, password: String, rootPath: String) {
        viewModelScope.launch {
            try {
                val config = WebDavConfig(
                    baseUrl = url.trimEnd('/'),
                    username = username.ifBlank { null },
                    password = password.ifBlank { null },
                    displayName = name,
                    rootPath = rootPath.ifBlank { "/" }
                )
                val sourceId = "webdav_${System.currentTimeMillis()}"
                val configJson = Json.encodeToString(config)
                app.sourceManager.addSource(
                    SourceEntity(
                        id = sourceId,
                        name = name,
                        type = SourceType.WEBDAV.name,
                        configJson = configJson,
                        enabled = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(importResult = "WebDAV 源添加成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(importResult = "保存失败: ${e.message}") }
            }
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
                localSource.importManager.importSingleVideo(uri)
                _uiState.update { it.copy(importResult = "成功导入 1 个视频") }
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
                _uiState.update { it.copy(importResult = "成功导入 ${result.episodeCount} 个剧集") }
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
