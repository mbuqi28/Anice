package com.aku.anice.data.remote

import android.util.Base64
import android.util.Log
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.Episode
import com.aku.anice.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class JsoupScraper {

    suspend fun getLatestAnime(query: String? = null, page: Int = 1, status: String = "", order: String = "update") = withContext(Dispatchers.IO) {
        val animeList = mutableListOf<Anime>()
        try {
            val url = if (query.isNullOrEmpty()) {
                "https://anichin.cafe/seri/?status=$status&type=donghua&order=$order&page=$page"
            } else {
                "https://anichin.cafe/page/$page/?s=$query"
            }
            
            val doc = Jsoup.connect(url).userAgent(USER_AGENT).get()
            val elements = doc.select("div.listupd article.bs")
            elements.forEach { element ->
                val title = element.select("div.tt").text()
                val thumb = element.select("img").attr("src")
                val link = element.select("a").attr("href")
                if (title.isNotEmpty()) animeList.add(Anime(title, thumb, link))
            }
        } catch (e: Exception) {
            Log.e("JsoupScraper", "Error getLatestAnime: ${e.message}")
        }
        animeList
    }

    suspend fun getEpisodeList(url: String) = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<Episode>()
        try {
            var doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(20000).get()
            var elements = doc.select("div.eplister ul li")
            
            if (elements.isEmpty()) {
                val seriesLink = doc.select("div.breadcrumb span[itemprop=itemListElement] a").let { links ->
                    if (links.size >= 2) links[1].attr("href") else ""
                }.ifEmpty { doc.select("div.info-content div.spec span:contains(Series) a").attr("href") }

                if (seriesLink.isNotEmpty() && !seriesLink.contains("?")) {
                    doc = Jsoup.connect(seriesLink).userAgent(USER_AGENT).get()
                    elements = doc.select("div.eplister ul li")
                }
            }

            elements.forEach { element ->
                val epUrl = element.select("a").attr("href")
                val epNum = element.select("div.epl-num").text()
                val epTitle = element.select("div.epl-title").text()
                val epDate = element.select("span.date").text().ifEmpty { 
                    element.select("div.epl-date").text() 
                }
                
                if (epUrl.isNotEmpty()) {
                    episodes.add(Episode(id = epUrl, number = epNum, title = epTitle, url = epUrl, date = epDate))
                }
            }
        } catch (e: Exception) {
            Log.e("JsoupScraper", "Error getEpisodeList: ${e.message}")
        }
        episodes.reversed()
    }

    suspend fun getVideoSources(episodeUrl: String) = withContext(Dispatchers.IO) {
        val sources = mutableListOf<VideoSource>()
        try {
            val doc = Jsoup.connect(episodeUrl).userAgent(USER_AGENT).get()

            // 1. Dropdown Mirror
            doc.select("select.mirror option").forEach { element ->
                processServerValue(element.text(), element.attr("value"), sources)
            }

            // 2. Mirror ID Buttons
            doc.select("div.mirror_id button, ul.mrtoggle li").forEach { element ->
                val name = element.text().ifEmpty { "Premium Server" }
                val value = element.attr("data-value").ifEmpty { element.attr("value") }
                processServerValue(name, value, sources)
            }
        } catch (e: Exception) {
            Log.e("JsoupScraper", "Error getVideoSources: ${e.message}")
        }
        sources.distinctBy { it.url }
    }

    private fun processServerValue(name: String, value: String, sources: MutableList<VideoSource>) {
        if (value.isEmpty() || value == "0") return
        var finalUrl = value
        if (!value.startsWith("http")) {
            try {
                val decodedBytes = Base64.decode(value, Base64.DEFAULT)
                val decodedString = String(decodedBytes)
                val iframeDoc = Jsoup.parse(decodedString)
                finalUrl = iframeDoc.select("iframe").attr("src")
            } catch (e: Exception) { }
        }
        if (finalUrl.startsWith("http")) sources.add(VideoSource(name, finalUrl))
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
}
