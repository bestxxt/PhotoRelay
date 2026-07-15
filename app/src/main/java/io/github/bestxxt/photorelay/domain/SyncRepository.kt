package io.github.bestxxt.photorelay.domain

import io.github.bestxxt.photorelay.data.network.AssetResponseDto
import io.github.bestxxt.photorelay.data.network.MetadataSearchDto
import io.github.bestxxt.photorelay.di.AppContainer

class SyncRepository(
    private val appContainer: AppContainer,
    private val authManager: AuthManager
) {

    suspend fun getRecentAssets(page: Int = 1, size: Int = 50, takenAfter: String? = null): Result<List<AssetResponseDto>> {
        val serverUrl = authManager.cachedServerUrl ?: return Result.failure(Exception("No server URL configured"))
        
        return try {
            val apiService = appContainer.createApiService(serverUrl)
            
            if (size == 10000) {
                // Handle "Max" by fetching all pages up to a reasonable limit (e.g. 10000 items)
                val allItems = mutableListOf<AssetResponseDto>()
                var currentPage = 1
                var hasMore = true
                val pageSize = 1000 // Immich usually accepts max 1000
                
                val formattedTakenAfter = takenAfter?.let { if (it.length == 10) "${it}T00:00:00.000Z" else it }
                
                while (hasMore && allItems.size < 10000) {
                    val request = MetadataSearchDto(
                        type = "IMAGE",
                        withExif = true,
                        page = currentPage,
                        size = pageSize,
                        takenAfter = formattedTakenAfter
                    )
                    val response = apiService.searchMetadata(request)
                    allItems.addAll(response.assets.items)
                    
                    if (response.assets.items.size < pageSize) {
                        hasMore = false
                    } else {
                        currentPage++
                    }
                }
                Result.success(allItems)
            } else {
                val formattedTakenAfter = takenAfter?.let { if (it.length == 10) "${it}T00:00:00.000Z" else it }
                val request = MetadataSearchDto(
                    type = "IMAGE",
                    withExif = true,
                    page = page,
                    size = size,
                    takenAfter = formattedTakenAfter
                )
                
                val response = apiService.searchMetadata(request)
                Result.success(response.assets.items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getServerUrl(): String? {
        return authManager.cachedServerUrl
    }
}
