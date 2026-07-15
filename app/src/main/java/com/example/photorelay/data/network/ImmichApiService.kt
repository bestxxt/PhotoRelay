package com.example.photorelay.data.network

import com.example.photorelay.data.network.AssetResponseDto
import com.example.photorelay.data.network.LoginRequest
import com.example.photorelay.data.network.LoginResponse
import com.example.photorelay.data.network.MetadataSearchDto
import com.example.photorelay.data.network.SearchResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ImmichApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/search/metadata")
    suspend fun searchMetadata(@Body request: MetadataSearchDto): SearchResponseDto

    @Streaming
    @GET("/api/assets/{id}/original")
    suspend fun downloadAsset(@Path("id") id: String): ResponseBody
}
