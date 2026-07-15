package io.github.bestxxt.photorelay.domain

import android.content.Context
import android.net.Uri
import io.github.bestxxt.photorelay.data.local.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CleanupManager(
    private val context: Context,
    private val appSettings: AppSettings
) {
    
    suspend fun runCleanup(keepDays: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val recordsMap = appSettings.syncRecords.first()
            val cutoffMillis = System.currentTimeMillis() - (keepDays * 24L * 60L * 60L * 1000L)
            
            val toClean = recordsMap.values.filter { record ->
                !record.isCleaned && record.downloadedAt <= cutoffMillis
            }
            
            if (toClean.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            var cleanedCount = 0
            val cleanedIds = mutableListOf<String>()
            
            for (record in toClean) {
                try {
                    val uri = Uri.parse(record.localUri)
                    context.contentResolver.delete(uri, null, null)
                    
                    // We mark it as cleaned regardless of if it actually existed
                    // because if it's gone, it's effectively "cleaned".
                    cleanedIds.add(record.assetId)
                    cleanedCount++
                } catch (e: Exception) {
                    // Ignore individual deletion errors
                }
            }
            
            if (cleanedIds.isNotEmpty()) {
                appSettings.markAsCleaned(cleanedIds)
            }
            
            Result.success(cleanedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
