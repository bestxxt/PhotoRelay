package io.github.bestxxt.photorelay.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.bestxxt.photorelay.data.local.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appSettings: AppSettings
) : ViewModel() {

    val autoSyncEnabled = appSettings.autoSyncEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val syncIntervalHours = appSettings.syncIntervalHours.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 24
    )
    val syncStartDate = appSettings.syncStartDate.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "2024-01-01"
    )
    val saveLocation = appSettings.saveLocation.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "PhotoRelay"
    )
    val keepDays = appSettings.keepDays.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 30
    )
    val appLogs = appSettings.appLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.saveAutoSyncEnabled(enabled)
            // WorkScheduler will observe this if we make it a flow, or we call it from UI.
        }
    }

    fun setSyncIntervalHours(hours: Int) {
        viewModelScope.launch { appSettings.saveSyncIntervalHours(hours) }
    }

    fun setSyncStartDate(date: String) {
        viewModelScope.launch { appSettings.saveSyncStartDate(date) }
    }

    fun setSaveLocation(location: String) {
        viewModelScope.launch { appSettings.saveLocation(location) }
    }

    fun setKeepDays(days: Int) {
        viewModelScope.launch { 
            // Enforce minimum 1 day to prevent 0-day instant cleanup bug
            appSettings.saveKeepDays(days.coerceAtLeast(1)) 
        }
    }

    fun clearSyncRecords() {
        viewModelScope.launch { appSettings.clearSyncRecords() }
    }

    fun clearAppLogs() {
        viewModelScope.launch { appSettings.clearAppLogs() }
    }

    class Factory(
        private val appSettings: AppSettings
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(appSettings) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
