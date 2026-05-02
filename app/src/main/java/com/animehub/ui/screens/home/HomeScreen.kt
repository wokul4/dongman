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
        topBar = {
            TopAppBar(title = { Text("AnimeHub") })
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recent Watch Section
            item {
                Text(
                    text = "最近观看",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (uiState.recentHistory.isEmpty()) {
                item {
                    Text(
                        text = "还没有观看记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            } else {
                items(uiState.recentHistory.take(5)) { history ->
                    AnimeCard(
                        title = "剧集 ${history.episodeId.take(20)}...",
                        subtitle = formatTime(history.progressMs),
                        onClick = { onNavigateToPlayer(history.episodeId, history.animeId, history.sourceId) }
                    )
                }
            }

            // Favorites Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "收藏",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (uiState.favorites.isEmpty()) {
                item {
                    EmptyState(
                        message = "还没有收藏",
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        modifier = Modifier.height(120.dp)
                    )
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "已启用源 (${uiState.enabledSources.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(uiState.enabledSources) { source ->
                Text(
                    text = "${source.name} (${source.type})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}时${minutes % 60}分"
        minutes > 0 -> "${minutes}分${seconds % 60}秒"
        else -> "${seconds}秒"
    }
}
