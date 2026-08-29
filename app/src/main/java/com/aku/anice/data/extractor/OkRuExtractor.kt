package com.aku.anice.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class OkRuExtractor : StreamExtractor {
    override val name: String = "OK.ru"

    override fun canExtract(url: String): Boolean {
        return url.contains("ok.ru") || url.contains("odnoklassniki")
    }

    override suspend fun extract(url: String): VideoStream? = withContext(Dispatchers.IO) {
        try {
            val response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://ok.ru/")
                .timeout(10000)
                .execute()
            
            val html = response.body()
            val hlsRegex = """hlsManifestUrl["&quot;]*:[:&quot;]*(https?:[^&"']+?\.m3u8[^&"']*)""".toRegex()
            val cleanHtml = html.replace("\\u0026", "&").replace("&amp;", "&").replace("\\/", "/").replace("&quot;", "\"")
            
            var match = hlsRegex.find(cleanHtml)?.groupValues?.get(1)
            
            if (match == null) {
                val metadataRegex = """metadata["']?\s*[:=]\s*["'](\{.*?\})["']""".toRegex()
                val metadataJson = metadataRegex.find(cleanHtml)?.groupValues?.get(1)
                if (metadataJson != null) {
                    match = hlsRegex.find(metadataJson.replace("\\\"", "\""))?.groupValues?.get(1)
                }
            }
            
            if (match != null) {
                return@withContext VideoStream(
                    url = match.replace(" ", ""),
                    headers = mapOf(
                        "Referer" to "https://ok.ru/",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("OkRuExtractor", "Error: ${e.message}")
        }
        null
    }
}
