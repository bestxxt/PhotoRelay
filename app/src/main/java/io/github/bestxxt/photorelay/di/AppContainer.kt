package io.github.bestxxt.photorelay.di

import android.content.Context
import io.github.bestxxt.photorelay.data.local.AppSettings
import io.github.bestxxt.photorelay.data.network.AuthInterceptor
import io.github.bestxxt.photorelay.data.network.ImmichApiService
import io.github.bestxxt.photorelay.domain.AuthManager
import io.github.bestxxt.photorelay.domain.CleanupManager
import io.github.bestxxt.photorelay.domain.MediaDownloader
import io.github.bestxxt.photorelay.domain.SyncRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val appSettings: AppSettings by lazy {
        AppSettings(context)
    }

    private val authInterceptor by lazy {
        AuthInterceptor { authManager.cachedToken }
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createApiService(baseUrl: String): ImmichApiService {
        // Ensure url ends with /
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImmichApiService::class.java)
    }

    val authManager: AuthManager by lazy {
        AuthManager(appSettings, this)
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(this, authManager)
    }

    val mediaDownloader: MediaDownloader by lazy {
        MediaDownloader(context, this, authManager, appSettings)
    }

    val cleanupManager: CleanupManager by lazy {
        CleanupManager(context, appSettings)
    }
}
