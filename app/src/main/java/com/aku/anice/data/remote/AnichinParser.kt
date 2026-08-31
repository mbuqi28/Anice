package com.aku.anice.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.AnimeDetail
import com.aku.anice.data.model.Episode
import com.aku.anice.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AnichinParser(private val context: Context) {

    private val client = NetworkClient.getClient(context)

    suspend fun getLatestReleases(page: Int = 1): List<Anime> = withContext(Dispatchers.IO) {
        val url = "https://anichin.cafe/seri/?type=donghua&order=update&page=$page"
        val html = fetchHtml(url) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        
        doc.select(".listupd .bsx").mapNotNull { element ->
            val title = element.select(".tt").text()
            val thumb = element.select("img").attr("src")
            val link = element.select("a").attr("href")
            val epTag = element.select(".bt .epx").text()
            if (title.isNotEmpty()) Anime(title, thumb, link) else null
        }
    }

    suspend fun getAnimeDetail(url: String): AnimeDetail? = withContext(Dispatchers.IO) {
        val html = fetchHtml(url) ?: return@withContext null
        val doc = Jsoup.parse(html)
        
        val title = doc.select("h1.entry-title").text()
        val episodes = doc.select(".eplister li").map { element ->
            val epUrl = element.select("a").attr("href")
            val epNum = element.select(".epl-num").text()
            val epTitle = element.select(".epl-title").text()
            val epDate = element.select(".epl-date").text()
            Episode(id = epUrl, number = epNum, title = epTitle, url = epUrl, date = epDate)
        }.reversed()
        
        AnimeDetail(title, episodes)
    }

    suspend fun getVideoServers(episodeUrl: String): List<VideoSource> = withContext(Dispatchers.IO) {
        val html = fetchHtml(episodeUrl) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        val sources = mutableListOf<VideoSource>()

        // Extract from select.mirror
        doc.select("select.mirror option").forEach { element ->
            val name = element.text()
            val value = element.attr("value")
            if (value.isNotEmpty() && value != "0") {
                val decodedUrl = decodeServerUrl(value)
                if (decodedUrl.isNotEmpty()) {
                    sources.add(VideoSource(name, decodedUrl))
                }
            }
        }
        
        // Extract from mirror_id buttons
        doc.select(".mirror_id button").forEach { element ->
            val name = element.text()
            val value = element.attr("data-value")
            if (value.isNotEmpty()) {
                val decodedUrl = decodeServerUrl(value)
                if (decodedUrl.isNotEmpty()) {
                    sources.add(VideoSource(name, decodedUrl))
                }
            }
        }

        sources.distinctBy { it.url }
    }

    private fun decodeServerUrl(value: String): String {
        return try {
            if (value.startsWith("http")) return value
            val decoded = String(Base64.decode(value, Base64.DEFAULT))
            if (decoded.contains("iframe")) {
                Jsoup.parse(decoded).select("iframe").attr("src")
            } else decoded
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun fetchHtml(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NetworkClient.USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e("AnichinParser", "Error fetching $url: ${e.message}")
            null
        }
    }
}
