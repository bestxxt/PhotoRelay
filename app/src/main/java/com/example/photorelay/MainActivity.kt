package com.example.photorelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.photorelay.ui.home.HomeScreen
import com.example.photorelay.ui.home.HomeViewModel
import com.example.photorelay.ui.login.LoginScreen
import com.example.photorelay.ui.login.LoginViewModel
import com.example.photorelay.ui.settings.SettingsScreen
import com.example.photorelay.ui.settings.SettingsViewModel
import com.example.photorelay.ui.theme.PhotoRelayTheme
import kotlinx.coroutines.launch

enum class AppScreen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as PhotoRelayApplication
        val authManager = app.container.authManager
        val syncRepository = app.container.syncRepository
        val appSettings = app.container.appSettings
        val mediaDownloader = app.container.mediaDownloader
        val cleanupManager = app.container.cleanupManager
        
        val loginViewModel: LoginViewModel by viewModels { LoginViewModel.Factory(authManager, appSettings) }
        val homeViewModel: HomeViewModel by viewModels { HomeViewModel.Factory(syncRepository, mediaDownloader, cleanupManager, appSettings) }
        val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory(appSettings) }

        setContent {
            PhotoRelayTheme {


                val coroutineScope = rememberCoroutineScope()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
                    
                    LaunchedEffect(Unit) {
                        isLoggedIn = authManager.hasValidToken()
                    }
                    if (isLoggedIn == null) {
                        // Loading state
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Loading...")
                        }
                    } else if (isLoggedIn == true) {
                        var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                        if (currentScreen == AppScreen.HOME) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                                onLogout = {
                                    coroutineScope.launch {
                                        authManager.logout()
                                        isLoggedIn = false
                                    }
                                }
                            )
                        } else {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { currentScreen = AppScreen.HOME },
                                onLogout = {
                                    coroutineScope.launch {
                                        authManager.logout()
                                        isLoggedIn = false
                                        currentScreen = AppScreen.HOME
                                    }
                                }
                            )
                        }
                    } else {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = { isLoggedIn = true }
                        )
                    }
                }
            }
        }
    }
}