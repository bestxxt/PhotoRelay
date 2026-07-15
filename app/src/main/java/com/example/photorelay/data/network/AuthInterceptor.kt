package com.example.photorelay.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        
        tokenProvider()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        // Ensure Accept header is json
        requestBuilder.addHeader("Accept", "application/json")
        
        return chain.proceed(requestBuilder.build())
    }
}
