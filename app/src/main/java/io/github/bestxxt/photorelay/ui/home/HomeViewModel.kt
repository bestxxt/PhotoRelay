package io.github.bestxxt.photorelay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.bestxxt.photorelay.data.network.AssetResponseDto
import io.github.bestxxt.photorelay.data.local.AppSettings
import io.github.bestxxt.photorelay.data.local.SyncRecord
import io.github.bestxxt.photorelay.domain.CleanupManager
import io.github.bestxxt.photorelay.domain.MediaDownloader
import io.github.bestxxt.photorelay.domain.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(
        val assets: List<AssetResponseDto>, 
        val serverUrl: String,
        val takenAfter: String? = null,
        val fetchSize: Int = 10000,
        val isSyncing: Boolean = false,
        val syncProgress: String = ""
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val syncRepository: SyncRepository,
    private val mediaDownloader: MediaDownloader,
    private val cleanupManager: CleanupManager,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _syncRecords = MutableStateFlow<Map<String, SyncRecord>>(emptyMap())
    val syncRecords: StateFlow<Map<String, SyncRecord>> = _syncRecords.asStateFlow()

    private val _keepDays = MutableStateFlow<Int>(30)
    val keepDays: StateFlow<Int> = _keepDays.asStateFlow()
    
    private val _homeDateSelection = MutableStateFlow<String>("7d")
    val homeDateSelection: StateFlow<String> = _homeDateSelection.asStateFlow()

    private val _homeSizeSelection = MutableStateFlow<Int>(10000)
    val homeSizeSelection: StateFlow<Int> = _homeSizeSelection.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.syncRecords.collectLatest { records ->
                _syncRecords.value = records
            }
        }
        viewModelScope.launch {
            appSettings.keepDays.collectLatest { days ->
                _keepDays.value = days
            }
        }
        viewModelScope.launch {
            appSettings.homeDateSelection.collectLatest { selection ->
                _homeDateSelection.value = selection
            }
        }
        viewModelScope.launch {
            appSettings.homeSizeSelection.collectLatest { size ->
                _homeSizeSelection.value = size
            }
        }
    }

    fun fetchAssets(takenAfter: String? = null, size: Int = 10000) {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val result = syncRepository.getRecentAssets(page = 1, size = size, takenAfter = takenAfter)
            if (result.isSuccess) {
                val url = syncRepository.getServerUrl() ?: ""
                _uiState.value = HomeUiState.Success(
                    assets = result.getOrDefault(emptyList()), 
                    serverUrl = url,
                    takenAfter = takenAfter,
                    fetchSize = size
                )
            } else {
                _uiState.value = HomeUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun saveHomeSelections(dateSelection: String, sizeSelection: Int) {
        viewModelScope.launch {
            appSettings.saveHomeDateSelection(dateSelection)
            appSettings.saveHomeSizeSelection(sizeSelection)
        }
    }
    
    fun syncAll() {
        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success) return
        
        val assetsToSync = currentState.assets.filter { 
            val record = syncRecords.value[it.id]
            record == null // Only sync if we have no record of it
        }
        if (assetsToSync.isEmpty()) return

        _uiState.value = currentState.copy(isSyncing = true, syncProgress = "0/${assetsToSync.size}")
        
        viewModelScope.launch {
            var successCount = 0
            for ((index, asset) in assetsToSync.withIndex()) {
                _uiState.value = currentState.copy(isSyncing = true, syncProgress = "$index/${assetsToSync.size}")
                val result = mediaDownloader.downloadAndSaveImage(
                    assetId = asset.id,
                    fileName = asset.originalFileName
                )
                if (result.isSuccess) {
                    successCount++
                }
            }
            _uiState.value = currentState.copy(isSyncing = false, syncProgress = "Finished: $successCount/${assetsToSync.size}")
        }
    }

    fun runCleanup(keepDays: Int = 30) {
        viewModelScope.launch {
            appSettings.saveKeepDays(keepDays)
            cleanupManager.runCleanup(keepDays)
        }
    }

    class Factory(
        private val syncRepository: SyncRepository,
        private val mediaDownloader: MediaDownloader,
        private val cleanupManager: CleanupManager,
        private val appSettings: AppSettings
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(syncRepository, mediaDownloader, cleanupManager, appSettings) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
