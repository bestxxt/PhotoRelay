package io.github.bestxxt.photorelay.data.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String
)
