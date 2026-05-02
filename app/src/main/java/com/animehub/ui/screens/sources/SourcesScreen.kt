package com.animehub.ui.screens.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animehub.domain.source.SourceType
import com.animehub.util.VideoFileDetector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onNavigateToBrowser: (sourceId: String) -> Unit = {},
    viewModel: SourcesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddMenu by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importLocalVideos(uris)
    }

    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val title = uri.lastPathSegment?.substringAfterLast('/') ?: "导入目录"
            viewModel.importLocalTreeUri(uri, title)
        }
    }

    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("源管理") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMenu = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加源")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            "仅支持合法授权内容和用户自有媒体",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                item {
                    Text("已添加源 (${uiState.sources.size})", style = MaterialTheme.typography.titleLarge)
                }

                if (uiState.sources.isEmpty()) {
                    item {
                        Text(
                            "暂无源，点击右下角 + 添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.sources, key = { it.id }) { source ->
                    SourceItem(
                        name = source.name,
                        type = source.type,
                        enabled = source.enabled,
                        onToggle = { viewModel.setEnabled(source.id, it) },
                        onDelete = { viewModel.deleteSource(source.id) },
                        isWebDav = source.type == SourceType.WEBDAV.name,
                        onBrowse = { onNavigateToBrowser(source.id) }
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text("预留源类型 (即将支持)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SourceType.entries.filter { it != SourceType.LOCAL_FILE && it != SourceType.WEBDAV }.forEach { type ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("${type.displayName} - 待实现", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showAddMenu,
                onDismissRequest = { showAddMenu = false },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                DropdownMenuItem(
                    text = { Text("导入视频文件") },
                    onClick = { showAddMenu = false; videoPickerLauncher.launch(VideoFileDetector.getSupportedMimeTypes()) },
                    leadingIcon = { Icon(Icons.Filled.VideoLibrary, null) }
                )
                DropdownMenuItem(
                    text = { Text("导入视频目录") },
                    onClick = { showAddMenu = false; treePickerLauncher.launch(null) },
                    leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) }
                )
                DropdownMenuItem(
                    text = { Text("添加 WebDAV 源") },
                    onClick = { showAddMenu = false; showWebDavDialog = true },
                    leadingIcon = { Icon(Icons.Filled.Cloud, null) }
                )
            }
        }
    }

    if (showWebDavDialog) {
        WebDavConfigDialog(
            onDismiss = { showWebDavDialog = false },
            onSave = { name, url, username, password, rootPath ->
                showWebDavDialog = false
                viewModel.saveWebDavSource(name, url, username, password, rootPath)
            },
            onTest = { url, username, password, rootPath ->
                viewModel.testWebDavConnection(url, username, password, rootPath)
            }
        )
    }
}

@Composable
private fun SourceItem(
    name: String,
    type: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    isWebDav: Boolean = false,
    onBrowse: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                    Text(text = type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (isWebDav && enabled) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Folder, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("浏览 WebDAV 目录")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除源") },
            text = { Text("确定要删除这个源吗？已导入的数据不会被删除。") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}
