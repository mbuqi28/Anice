package com.aku.anice.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class DailymotionExtractor : StreamExtractor {
    override val name: String = "Dailymotion"

    override fun canExtract(url: String): Boolean {
        return url.contains("dailymotion.com")
    }

    override suspend fun extract(url: String): VideoStream? = withContext(Dispatchers.IO) {
        try {
            val videoId = url.split("/").lastOrNull()?.split("?")?.firstOrNull() ?: return@withContext null
            val metadataUrl = "https://www.dailymotion.com/player/metadata/video/$videoId"
            
            val response = Jsoup.connect(metadataUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://geo.dailymotion.com/")
                .ignoreContentType(true)
                .execute()
            
            val json = response.body()
            // Cari manifest URL di JSON metadata
            val regex = """"(https?://[^"]+m3u8[^"]+)"""".toRegex()
            val match = regex.find(json)?.groupValues?.get(1)
            
            if (match != null) {
                return@withContext VideoStream(
                    url = match.replace("\\/", "/").replace("\\u0026", "&"),
                    headers = mapOf(
                        "Referer" to "https://geo.dailymotion.com/",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("DailymotionExtractor", "Error: ${e.message}")
        }
        null
    }
}
