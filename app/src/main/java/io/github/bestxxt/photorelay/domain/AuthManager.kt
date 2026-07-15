package io.github.bestxxt.photorelay.domain

import io.github.bestxxt.photorelay.data.local.AppSettings
import io.github.bestxxt.photorelay.data.network.LoginRequest
import io.github.bestxxt.photorelay.di.AppContainer
import kotlinx.coroutines.flow.first

class AuthManager(
    private val appSettings: AppSettings,
    private val appContainer: AppContainer
) {
    
    var cachedToken: String? = null
        private set

    var cachedServerUrl: String? = null
        private set

    suspend fun login(url: String, email: String, password: String): Result<Unit> {
        return try {

            val apiService = appContainer.createApiService(url)
            val request = LoginRequest(email, password)
            val response = apiService.login(request)
            
            // Save url, token, and email
            appSettings.saveServerUrl(url)
            appSettings.saveAccessToken(response.accessToken)
            appSettings.saveLastEmail(email)
            
            cachedToken = response.accessToken
            cachedServerUrl = url
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasValidToken(): Boolean {
        val token = appSettings.accessToken.first()
        val url = appSettings.serverUrl.first()
        
        cachedToken = token
        cachedServerUrl = url
        
        return !token.isNullOrEmpty()
    }

    suspend fun logout() {
        appSettings.clearAuthData()
        cachedToken = null
    }
}
