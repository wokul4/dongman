package com.animehub.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animehub.ui.components.AnimeCard
import com.animehub.ui.components.EmptyState
import com.animehub.ui.components.ErrorState
import com.animehub.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToDetail: (animeId: String, sourceId: String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("搜索") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索番剧...") },
                trailingIcon = {
                    IconButton(onClick = { viewModel.search() }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    EmptyState(
                        message = "输入关键词搜索番剧",
                        icon = Icons.Filled.Search,
                        modifier = Modifier.weight(1f)
                    )
                }
                is SearchUiState.Loading -> {
                    LoadingState(modifier = Modifier.weight(1f))
                }
                is SearchUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.results) { anime ->
                            AnimeCard(
                                title = anime.title,
                                subtitle = "来源: ${anime.sourceId}",
                                coverUrl = anime.coverUrl,
                                onClick = { onNavigateToDetail(anime.id, anime.sourceId) }
                            )
                        }
                    }
                }
                is SearchUiState.Empty -> {
                    EmptyState(
                        message = "没有找到结果",
                        modifier = Modifier.weight(1f)
                    )
                }
                is SearchUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.search() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
