package com.aku.anice.data.extractor

data class VideoStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val isHls: Boolean = url.contains(".m3u8")
)

interface StreamExtractor {
    val name: String
    suspend fun extract(url: String): VideoStream?
    fun canExtract(url: String): Boolean
}
