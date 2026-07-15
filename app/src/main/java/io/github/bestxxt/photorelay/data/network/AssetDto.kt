package io.github.bestxxt.photorelay.data.network

import com.google.gson.annotations.SerializedName

data class MetadataSearchDto(
    @SerializedName("page") val page: Int? = null,
    @SerializedName("size") val size: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("takenAfter") val takenAfter: String? = null,
    @SerializedName("takenBefore") val takenBefore: String? = null,
    @SerializedName("order") val order: String? = "desc",
    @SerializedName("withExif") val withExif: Boolean? = false
)

data class SearchResponseDto(
    @SerializedName("assets") val assets: SearchAssetResponseDto
)

data class SearchAssetResponseDto(
    @SerializedName("items") val items: List<AssetResponseDto>,
    @SerializedName("count") val count: Int
)

data class AssetResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("originalFileName") val originalFileName: String,
    @SerializedName("fileCreatedAt") val fileCreatedAt: String,
    @SerializedName("type") val type: String,
    @SerializedName("checksum") val checksum: String?
)
