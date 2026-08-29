package com.aku.anice.data.extractor

class StreamExtractorManager {
    private val extractors = listOf(
        OkRuExtractor(),
        DailymotionExtractor(),
        AnichinStreamExtractor()
    )

    suspend fun extract(url: String): VideoStream? {
        val extractor = extractors.find { it.canExtract(url) }
        return extractor?.extract(url)
    }
}
