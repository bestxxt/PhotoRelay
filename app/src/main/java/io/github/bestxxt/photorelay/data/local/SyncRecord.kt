package io.github.bestxxt.photorelay.data.local

data class SyncRecord(
    val assetId: String,
    val localUri: String,
    val downloadedAt: Long,
    val isCleaned: Boolean = false
)
