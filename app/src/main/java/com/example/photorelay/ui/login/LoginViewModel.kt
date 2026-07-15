package com.example.photorelay.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.photorelay.domain.AuthManager
import com.example.photorelay.data.local.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val authManager: AuthManager,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val initialServerUrl: StateFlow<String?> = appSettings.serverUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    
    val initialEmail: StateFlow<String?> = appSettings.lastEmail.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    fun login(serverUrl: String, email: String, password: String) {
        if (serverUrl.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please fill in all fields")
            return
        }

        val url = if (!serverUrl.startsWith("http")) "http://$serverUrl" else serverUrl

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authManager.login(url, email, password)
            if (result.isSuccess) {
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    class Factory(
        private val authManager: AuthManager,
        private val appSettings: AppSettings
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(authManager, appSettings) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
