package com.aku.anice.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object VideoExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    suspend fun extract(url: String): VideoStream? = withContext(Dispatchers.IO) {
        Log.d("VideoExtractor", "Starting extraction for: $url")
        try {
            when {
                url.contains("ok.ru") || url.contains("odnoklassniki") -> extractOkRu(url)
                url.contains("dailymotion.com") -> extractDailymotion(url)
                url.contains("anichin.stream") -> extractAnichinStream(url)
                url.contains(".m3u8") || url.contains(".mp4") -> VideoStream(url, mapOf("User-Agent" to MOBILE_UA))
                else -> null
            }
        } catch (e: Exception) {
            Log.e("VideoExtractor", "Extraction error for $url: ${e.message}")
            null
        }
    }

    private fun extractOkRu(url: String): VideoStream? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Referer", "https://ok.ru/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val html = response.body?.string() ?: return null

            val dataOptions = Jsoup.parse(html).select("div[data-options]").attr("data-options")
            if (dataOptions.isEmpty()) return null

            val json = JSONObject(dataOptions.replace("&quot;", "\""))
            val flashvars = json.optJSONObject("flashvars") ?: return null

            val hlsUrl = flashvars.optString("hlsMasterPlaylistUrl")
            if (hlsUrl.isNotEmpty()) {
                return VideoStream(
                    url = hlsUrl.replace("\\u0026", "&"),
                    headers = mapOf(
                        "User-Agent" to MOBILE_UA,
                        "Referer" to "https://ok.ru/"
                    )
                )
            }

            val videos = flashvars.optJSONArray("videos")
            if (videos != null && videos.length() > 0) {
                val bestVideo = videos.getJSONObject(videos.length() - 1).getString("url")
                return VideoStream(
                    url = bestVideo,
                    headers = mapOf("User-Agent" to MOBILE_UA, "Referer" to "https://ok.ru/")
                )
            }
        }
        return null
    }

    private fun extractDailymotion(url: String): VideoStream? {
        val videoId = Regex("""(?:video|video/|video=)([a-zA-Z0-9]+)""").find(url)?.groupValues?.get(1)
            ?: url.split("/").lastOrNull()?.split("?")?.firstOrNull()
            ?: return null

        val metadataUrl = "https://www.dailymotion.com/player/metadata/video/$videoId"

        val request = Request.Builder()
            .url(metadataUrl)
            .header("User-Agent", MOBILE_UA)
            .header("Referer", "https://geo.dailymotion.com/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val jsonStr = response.body?.string() ?: return null
            val json = JSONObject(jsonStr)
            val qualities = json.optJSONObject("qualities") ?: return null
            val auto = qualities.optJSONArray("auto") ?: return null

            if (auto.length() > 0) {
                return VideoStream(
                    url = auto.getJSONObject(0).getString("url"),
                    headers = mapOf(
                        "User-Agent" to MOBILE_UA,
                        "Referer" to "https://www.dailymotion.com/",
                        "Origin" to "https://www.dailymotion.com"
                    )
                )
            }
        }
        return null
    }

    private fun extractAnichinStream(url: String): VideoStream? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Referer", "https://anichin.cafe/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val html = response.body?.string() ?: return null

            val streamUrl = Regex("""(https?://[^\s"']+\.(?:m3u8|mp4)[^\s"']*)""").find(html)?.value

            if (streamUrl != null) {
                return VideoStream(
                    url = streamUrl,
                    headers = mapOf(
                        "User-Agent" to MOBILE_UA,
                        "Referer" to "https://anichin.cafe/"
                    )
                )
            }
        }
        return null
    }
}
