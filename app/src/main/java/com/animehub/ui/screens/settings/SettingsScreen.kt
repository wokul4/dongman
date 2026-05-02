package com.animehub.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.clearResult) {
        uiState.clearResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Version
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App 版本", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        uiState.appVersion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cache management
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("缓存大小", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            uiState.cacheSize,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.showClearConfirm() },
                        enabled = !uiState.isCleaning,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (uiState.isCleaning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("清理缓存")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal disclaimer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "合法使用声明",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AnimeHub 仅支持合法来源的媒体内容播放，包括：\n" +
                                "• 用户自有的本地视频文件\n" +
                                "• 用户自有的 WebDAV/NAS 存储\n" +
                                "• 官方授权的 API 接口\n" +
                                "• 合法的 RSS/Atom 订阅\n\n" +
                                "本应用不提供任何盗版内容、不绕过 DRM、\n" +
                                "不破解付费墙、不抓取受版权保护的内容。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    // Clear cache confirmation dialog
    if (uiState.showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearConfirm() },
            title = { Text("清理缓存") },
            text = { Text("确定要清理缓存吗？数据库、收藏、观看历史和源配置不会被删除。") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCache() }) {
                    Text("确定清理")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}
