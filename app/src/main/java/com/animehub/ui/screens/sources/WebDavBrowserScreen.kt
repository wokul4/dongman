package com.animehub.ui.screens.sources

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animehub.AnimeHubApplication
import com.animehub.data.source.webdav.WebDavClient
import com.animehub.data.source.webdav.WebDavConfig
import com.animehub.data.source.webdav.WebDavItem
import com.animehub.util.VideoFileDetector
import kotlinx.coroutines.launch

sealed class BrowserUiState {
    data object Loading : BrowserUiState()
    data class Success(val items: List<WebDavItem>, val currentPath: String) : BrowserUiState()
    data class Error(val message: String) : BrowserUiState()
}

class WebDavBrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication

    var sourceId: String = ""
        private set
    private var source: WebDavClient? = null
    private var config: WebDavConfig? = null

    private val _uiState = MutableStateFlow<BrowserUiState>(BrowserUiState.Loading)
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _pathStack = mutableListOf<String>()

    fun init(sourceId: String) {
        this.sourceId = sourceId
        loadRoot()
    }

    fun loadRoot() {
        _pathStack.clear()
        _pathStack.add("/")
        loadDirectory("/")
    }

    fun enterDirectory(path: String) {
        val normalized = if (path.startsWith("/")) path else "/$path"
        _pathStack.add(normalized)
        loadDirectory(normalized)
    }

    fun goBack(): Boolean {
        if (_pathStack.size <= 1) return false
        _pathStack.removeAt(_pathStack.lastIndex)
        loadDirectory(_pathStack.last())
        return true
    }

    fun getCurrentPath(): String = _pathStack.lastOrNull() ?: "/"

    fun importDirectory(dirName: String) {
        val sourceMgr = app.sourceManager.getWebDavSource(sourceId) ?: return
        viewModelScope.launch {
            val count = sourceMgr.importDirectoryAsAnime(getCurrentPath(), dirName)
            _uiState.value = BrowserUiState.Success(
                items = (_uiState.value as? BrowserUiState.Success)?.items ?: emptyList(),
                currentPath = getCurrentPath()
            )
        }
    }

    fun importSingleFile(item: WebDavItem) {
        val sourceMgr = app.sourceManager.getWebDavSource(sourceId) ?: return
        viewModelScope.launch {
            val animeTitle = item.name.substringBeforeLast(".")
            sourceMgr.importSingleVideo(item, animeTitle)
        }
    }

    private fun loadDirectory(path: String) {
        val sourceMgr = app.sourceManager.getWebDavSource(sourceId) ?: run {
            // Try to create from source config
            viewModelScope.launch {
                val entity = app.database.sourceDao().getById(sourceId)
                if (entity != null && entity.type == "WEBDAV") {
                    try {
                        val cfg = kotlinx.serialization.json.Json.decodeFromString<WebDavConfig>(entity.configJson)
                        config = cfg
                        loadWithConfig(cfg, path)
                    } catch (_: Exception) {
                        _uiState.value = BrowserUiState.Error("配置解析失败")
                    }
                } else {
                    _uiState.value = BrowserUiState.Error("源未找到: $sourceId")
                }
            }
            return
        }
        config = sourceMgr.config
        loadWithConfig(sourceMgr.config, path)
    }

    private fun loadWithConfig(cfg: WebDavConfig, path: String) {
        val client = WebDavClient(cfg)
        viewModelScope.launch {
            _uiState.value = BrowserUiState.Loading
            when (val result = client.listDirectory(path)) {
                is WebDavClient.WebDavResult.Success -> {
                    _uiState.value = BrowserUiState.Success(result.data, path)
                }
                is WebDavClient.WebDavResult.Error -> {
                    _uiState.value = BrowserUiState.Error(result.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBrowserScreen(
    sourceId: String,
    onBack: () -> Unit,
    onNavigateToPlayer: (episodeId: String, animeId: String, sourceId: String) -> Unit,
    viewModel: WebDavBrowserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sourceId) {
        viewModel.init(sourceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 浏览") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (!viewModel.goBack()) onBack()
                    }) { Text("返回") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Path breadcrumb
            Text(
                text = "当前: ${viewModel.getCurrentPath()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val state = uiState) {
                is BrowserUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is BrowserUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Text(state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadRoot() }) { Text("重试") }
                        }
                    }
                }
                is BrowserUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Import directory button
                        if (state.items.any { it.isDirectory }) {
                            item {
                                OutlinedButton(
                                    onClick = {
                                        val dirName = viewModel.getCurrentPath().trimEnd('/').substringAfterLast('/')
                                        if (dirName.isNotBlank()) viewModel.importDirectory(dirName)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Icon(Icons.Filled.CloudDownload, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("导入当前目录为番剧")
                                }
                            }
                        }

                        // Directory entries
                        items(state.items.filter { it.isDirectory }) { item ->
                            DirectoryItem(item = item) { viewModel.enterDirectory(item.path) }
                        }

                        // Video file entries
                        items(state.items.filter { !it.isDirectory && VideoFileDetector.isVideoFile(it.name) }) { item ->
                            VideoFileItem(
                                item = item,
                                onImport = { viewModel.importSingleFile(item) }
                            )
                        }

                        // No video files hint
                        if (state.items.none { !it.isDirectory && VideoFileDetector.isVideoFile(it.name) }) {
                            item {
                                Text(
                                    "当前目录没有视频文件",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryItem(item: WebDavItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VideoFileItem(item: WebDavItem, onImport: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                item.sizeBytes?.let {
                    Text(formatSize(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton(onClick = { showConfirm = true }) {
                Icon(Icons.Filled.CloudDownload, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("导入")
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("导入视频") },
            text = { Text("将 \"${item.name}\" 导入为番剧？") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onImport() }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            }
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}
