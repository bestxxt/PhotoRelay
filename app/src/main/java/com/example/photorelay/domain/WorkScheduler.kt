package com.example.photorelay.domain

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val SYNC_WORK_NAME = "PhotoRelaySyncWork"

    fun updateSyncWork(context: Context, enabled: Boolean, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        
        if (!enabled) {
            workManager.cancelUniqueWork(SYNC_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if exists and interval changed
            syncRequest
        )
    }
}
