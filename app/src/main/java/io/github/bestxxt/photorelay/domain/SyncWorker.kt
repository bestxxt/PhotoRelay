package io.github.bestxxt.photorelay.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.bestxxt.photorelay.PhotoRelayApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as PhotoRelayApplication
        val authManager = app.container.authManager
        val syncRepository = app.container.syncRepository
        val appSettings = app.container.appSettings
        val mediaDownloader = app.container.mediaDownloader
        val cleanupManager = app.container.cleanupManager

        if (!authManager.hasValidToken()) {
            return@withContext Result.failure()
        }

        try {
            val syncStartDate = appSettings.syncStartDate.first()
            val keepDays = appSettings.keepDays.first()
            val saveLocation = appSettings.saveLocation.first()
            val syncRecords = appSettings.syncRecords.first()

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val startTime = sdf.format(java.util.Date())
            appSettings.addAppLog("[$startTime] Background Sync Triggered")

            // 1. Fetch Max assets from syncStartDate
            val fetchResult = syncRepository.getRecentAssets(size = 10000, takenAfter = syncStartDate)
            if (fetchResult.isFailure) {
                appSettings.addAppLog("↳ Failed to fetch recent assets from server.")
                return@withContext Result.retry()
            }
            val assets = fetchResult.getOrNull() ?: emptyList()

            // 2. Filter out already synced or cleaned assets
            val newAssets = assets.filter { asset ->
                !syncRecords.containsKey(asset.id)
            }
            
            appSettings.addAppLog("↳ Found ${newAssets.size} new photos to sync.")

            // 3. Download new assets
            var successCount = 0
            for (asset in newAssets) {
                val result = mediaDownloader.downloadAndSaveImage(
                    assetId = asset.id,
                    fileName = asset.originalFileName,
                    albumName = saveLocation
                )
                if (result.isSuccess) successCount++
            }
            
            if (newAssets.isNotEmpty()) {
                appSettings.addAppLog("↳ Successfully downloaded $successCount/${newAssets.size} photos.")
            }

            // 4. Run cleanup
            cleanupManager.runCleanup(keepDays)
            appSettings.addAppLog("↳ Cleanup complete. Sync Finished.")

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            appSettings.addAppLog("↳ Sync Failed with Exception: ${e.message}")
            Result.retry()
        }
    }
}
