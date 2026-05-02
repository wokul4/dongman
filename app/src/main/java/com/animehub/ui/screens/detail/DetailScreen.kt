package com.animehub.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animehub.ui.components.AnimeCover
import com.animehub.ui.components.EmptyState
import com.animehub.ui.components.ErrorState
import com.animehub.ui.components.FavButton
import com.animehub.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    animeId: String,
    sourceId: String,
    onNavigateToPlayer: (episodeId: String, animeId: String, sourceId: String) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(animeId, sourceId) {
        viewModel.load(animeId, sourceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = uiState) {
                        is DetailUiState.Success -> s.detail.title
                        else -> "详情"
                    }
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                actions = {
                    if (uiState is DetailUiState.Success) {
                        FavButton(
                            isFavorite = (uiState as DetailUiState.Success).isFavorite,
                            onClick = { viewModel.toggleFavorite() }
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                LoadingState(modifier = Modifier.padding(padding))
            }
            is DetailUiState.Error -> {
                ErrorState(
                    message = state.message,
                    modifier = Modifier.padding(padding)
                )
            }
            is DetailUiState.Success -> {
                val detail = state.detail
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Info section
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (detail.coverUrl != null) {
                                    AnimeCover(
                                        coverUrl = detail.coverUrl,
                                        contentDescription = detail.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Text(
                                    text = detail.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (detail.description != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = detail.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "来源: ${detail.sourceId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Episodes section
                    item {
                        Text(
                            text = "剧集 (${detail.episodes.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (detail.episodes.isEmpty()) {
                        item {
                            EmptyState(
                                message = "暂无剧集",
                                modifier = Modifier.height(120.dp)
                            )
                        }
                    } else {
                        items(detail.episodes) { episode ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { onNavigateToPlayer(episode.id, detail.id, detail.sourceId) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = episode.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        episode.durationMs?.let { dur ->
                                            Text(
                                                text = formatDuration(dur),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "${min}:${sec.toString().padStart(2, '0')}"
}
