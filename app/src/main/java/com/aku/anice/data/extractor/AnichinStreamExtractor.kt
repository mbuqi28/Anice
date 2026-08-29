package com.aku.anice.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class AnichinStreamExtractor : StreamExtractor {
    override val name: String = "AnichinStream"

    override fun canExtract(url: String): Boolean {
        return url.contains("anichin.stream") || url.contains(".mp4") || url.contains(".m3u8")
    }

    override suspend fun extract(url: String): VideoStream? = withContext(Dispatchers.IO) {
        if (url.contains(".mp4") || url.contains(".m3u8")) {
            return@withContext VideoStream(
                url = url,
                headers = mapOf("Referer" to "https://anichin.cafe/")
            )
        }
        
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://anichin.cafe/")
                .get()
            
            val html = doc.html()
            val videoRegex = """(https?://[^\s"']+\.(?:m3u8|mp4)[^\s"']*)""".toRegex()
            val match = videoRegex.find(html)?.value?.takeIf { !it.contains("thumbnail") }
            
            if (match != null) {
                return@withContext VideoStream(
                    url = match,
                    headers = mapOf("Referer" to "https://anichin.cafe/")
                )
            }
        } catch (e: Exception) {
            Log.e("AnichinStreamExtractor", "Error: ${e.message}")
        }
        null
    }
}
