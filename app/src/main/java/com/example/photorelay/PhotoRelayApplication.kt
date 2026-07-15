package com.example.photorelay

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.photorelay.di.AppContainer

class PhotoRelayApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { container.okHttpClient }
            .build()
    }
}
