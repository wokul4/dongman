package com.animehub.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animehub.AnimeHubApplication
import com.animehub.data.cache.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appVersion: String = "1.0.0",
    val cacheSize: String = "计算中...",
    val isCleaning: Boolean = false,
    val showClearConfirm: Boolean = false,
    val clearResult: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AnimeHubApplication
    private val cacheManager = CacheManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val info = cacheManager.getCacheInfo()
            _uiState.value = _uiState.value.copy(cacheSize = info.totalFormatted)
        }
    }

    fun showClearConfirm() {
        _uiState.value = _uiState.value.copy(showClearConfirm = true)
    }

    fun dismissClearConfirm() {
        _uiState.value = _uiState.value.copy(showClearConfirm = false)
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true, showClearConfirm = false)
            val result = cacheManager.clearAll()
            result.fold(
                onSuccess = {
                    refreshCacheSize()
                    _uiState.value = _uiState.value.copy(
                        isCleaning = false,
                        clearResult = "清理完成"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isCleaning = false,
                        clearResult = "清理失败: ${e.message}"
                    )
                }
            )
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(clearResult = null)
    }
}
