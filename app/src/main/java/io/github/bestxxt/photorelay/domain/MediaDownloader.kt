package io.github.bestxxt.photorelay.domain

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.bestxxt.photorelay.data.local.AppSettings
import io.github.bestxxt.photorelay.data.local.SyncRecord
import io.github.bestxxt.photorelay.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class MediaDownloader(
    private val context: Context,
    private val appContainer: AppContainer,
    private val authManager: AuthManager,
    private val appSettings: AppSettings
) {

    suspend fun downloadAndSaveImage(
        assetId: String,
        fileName: String,
        albumName: String = "PhotoRelay",
        mimeType: String = "image/jpeg"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val serverUrl = authManager.cachedServerUrl ?: return@withContext Result.failure(Exception("No server URL configured"))
        try {
            val apiService = appContainer.createApiService(serverUrl)
            // 1. Fetch the stream from Immich
            val responseBody = apiService.downloadAsset(assetId)
            
            // 2. Prepare MediaStore insertion
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + albumName)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val contentResolver = context.contentResolver
            val imageUri: Uri? = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (imageUri == null) {
                return@withContext Result.failure(Exception("Failed to create MediaStore entry"))
            }

            // 3. Stream network response into MediaStore file
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // 4. Mark as completely written
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                }
                
                // 5. Save to synced list as SyncRecord
                val record = SyncRecord(
                    assetId = assetId,
                    localUri = imageUri.toString(),
                    downloadedAt = System.currentTimeMillis(),
                    isCleaned = false
                )
                appSettings.saveSyncRecord(record)
                
                Result.success(Unit)
            } catch (e: Exception) {
                // Clean up incomplete file if possible
                contentResolver.delete(imageUri, null, null)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
