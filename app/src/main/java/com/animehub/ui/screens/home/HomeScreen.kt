package com.animehub.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animehub.ui.components.AnimeCard
import com.animehub.ui.components.EmptyState
import com.animehub.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (animeId: String, sourceId: String) -> Unit,
    onNavigateToPlayer: (episodeId: String, animeId: String, sourceId: String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("AnimeHub") }) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recent Watch Section
            item {
                Text("最近观看", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (uiState.recentHistory.isEmpty()) {
                item {
                    Text("还没有观看记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(uiState.recentHistory.take(10)) { item ->
                    val h = item.history
                    RecentWatchCard(
                        episodeTitle = item.episodeTitle ?: "未知剧集",
                        animeTitle = item.animeTitle,
                        progressMs = h.progressMs,
                        durationMs = h.durationMs,
                        onClick = { onNavigateToPlayer(h.episodeId, h.animeId, h.sourceId) }
                    )
                }
            }

            // Favorites Section
            item {
                Spacer(Modifier.height(16.dp))
                Text("收藏", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (uiState.favorites.isEmpty()) {
                item {
                    EmptyState(message = "还没有收藏", icon = Icons.AutoMirrored.Filled.LibraryBooks, modifier = Modifier.height(120.dp))
                }
            } else {
                items(uiState.favorites) { anime ->
                    AnimeCard(
                        title = anime.title,
                        subtitle = "来源: ${anime.sourceId}",
                        coverUrl = anime.coverUrl,
                        onClick = { onNavigateToDetail(anime.id, anime.sourceId) }
                    )
                }
            }

            // Enabled Sources Section
            item {
                Spacer(Modifier.height(16.dp))
                Text("已启用源 (${uiState.enabledSources.size})", style = MaterialTheme.typography.titleLarge)
            }
            items(uiState.enabledSources) { source ->
                Text("${source.name} (${source.type})", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RecentWatchCard(
    episodeTitle: String,
    animeTitle: String?,
    progressMs: Long,
    durationMs: Long,
    onClick: () -> Unit
) {
    val progress = if (durationMs > 0L) (progressMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(episodeTitle, style = MaterialTheme.typography.titleSmall)
                    if (animeTitle != null) {
                        Text(animeTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (durationMs > 0L) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatTime(progressMs)} / ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "${m}:${s.toString().padStart(2, '0')}"
}
