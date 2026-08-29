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

    // --- LOGIKA EXTRACTION TINGKAT TINGGI ---

    suspend fun extractDirectLink(embedUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("Extractor", "Attempting: $embedUrl")
            return@withContext when {
                embedUrl.contains("ok.ru") || embedUrl.contains("odnoklassniki") -> extractOkRu(embedUrl)
                embedUrl.contains("vk.com") || embedUrl.contains("vkvideo") -> extractVk(embedUrl)
                embedUrl.contains("blogger.com") || embedUrl.contains("get-video") -> extractBlogger(embedUrl)
                embedUrl.contains("dailymotion.com") -> extractDailymotion(embedUrl)
                embedUrl.contains(".mp4") || embedUrl.contains(".m3u8") -> embedUrl
                else -> deepScanForVideo(embedUrl)
            }
        } catch (e: Exception) {
            Log.e("Extractor", "Failed: ${e.message}")
            null
        }
    }

    private suspend fun extractOkRu(url: String): String? {
        try {
            Log.d("Extractor", "OK.ru Pro Start: $url")
            
            // 1. Dapatkan Halaman Utama (Embed atau Video Page)
            val response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://ok.ru/")
                .timeout(10000)
                .execute()
            
            val html = response.body()
            
            // OK.ru sering menyimpan link di dalam atribut data-options atau dalam variabel JS metadata
            val hlsRegex = """hlsManifestUrl["&quot;]*:[:&quot;]*(https?:[^&"']+?\.m3u8[^&"']*)""".toRegex()
            
            // Bersihkan escape characters secara agresif
            val cleanHtml = html.replace("\\u0026", "&").replace("&amp;", "&").replace("\\/", "/").replace("&quot;", "\"")
            
            var match = hlsRegex.find(cleanHtml)?.groupValues?.get(1)
            
            if (match == null) {
                // Cari di dalam blok metadata yang mungkin ter-escape dua kali
                val metadataRegex = """metadata["']?\s*[:=]\s*["'](\{.*?\})["']""".toRegex()
                val metadataJson = metadataRegex.find(cleanHtml)?.groupValues?.get(1)
                if (metadataJson != null) {
                    match = hlsRegex.find(metadataJson.replace("\\\"", "\""))?.groupValues?.get(1)
                }
            }
            
            // JIKA MASIH NULL, cek apakah ada iframe VK di dalamnya (Kasus di Screenshot)
            if (match == null) {
                val iframeVk = Jsoup.parse(html).select("iframe[src*=vk]").attr("src")
                if (iframeVk.isNotEmpty()) {
                    val finalIframeUrl = when {
                        iframeVk.startsWith("http") -> iframeVk
                        iframeVk.startsWith("//") -> "https:$iframeVk"
                        else -> "https://vk.com$iframeVk"
                    }
                    Log.d("Extractor", "Found VK iframe inside OK.ru, redirecting to: $finalIframeUrl")
                    return extractVk(finalIframeUrl)
                }
            }
            
            if (match != null) {
                val finalUrl = match.replace(" ", "")
                Log.d("Extractor", "OK.ru Success: $finalUrl")
                return finalUrl
            }
            
            // 2. Fallback: API Metadata Internal
            val videoId = """(\d+)""".toRegex().findAll(url).lastOrNull()?.value
            if (videoId != null) {
                val metadataUrl = "https://ok.ru/dk?cmd=videoPlayerMetadata&mid=$videoId"
                val apiResponse = Jsoup.connect(metadataUrl)
                    .userAgent(USER_AGENT)
                    .header("Referer", url)
                    .ignoreContentType(true)
                    .execute().body()
                
                val apiMatch = hlsRegex.find(apiResponse.replace("\\u0026", "&").replace("\\/", "/").replace("&quot;", "\""))?.groupValues?.get(1)
                if (apiMatch != null) return apiMatch
            }
        } catch (e: Exception) {
            Log.e("Extractor", "OK.ru Pro Error: ${e.message}")
        }
        return null
    }

    private suspend fun extractVk(url: String): String? {
        try {
            val response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Referer", "https://vk.com/")
                .execute()
            val html = response.body()
            
            // VK sering menyimpan link di src tag atau dalam variabel JS
            val regex = """(https?://[^\s"']+\.mp4\?[^\s"']+)""".toRegex()
            return regex.find(html.replace("\\/", "/"))?.value
        } catch (e: Exception) { }
        return null
    }

    private suspend fun extractBlogger(url: String): String? {
        val doc = Jsoup.connect(url).userAgent(USER_AGENT).get()
        val scriptData = doc.select("script").html()
        val regex = """"(https://[^"]+)"""".toRegex()
        return regex.findAll(scriptData).lastOrNull()?.groupValues?.get(1)
            ?.replace("\\u003d", "=")?.replace("\\u0026", "&")
    }

    private suspend fun extractDailymotion(url: String): String? {
        try {
            val response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Referer", "https://anichin.cafe/")
                .execute()
            val html = response.body()
            
            // Dailymotion menyimpan manifest di dalam objek JSON 'config'
            // Kita cari .m3u8 yang memiliki token keamanan (sec)
            val regex = """"(https?://[^"]+m3u8[^"]+sec=[^"]+)"""".toRegex()
            var match = regex.find(html)?.groupValues?.get(1)
            
            if (match == null) {
                // Pola alternatif tanpa sec (kurang disukai tapi mungkin bekerja)
                val fallbackRegex = """"(https?://[^"]+m3u8[^"]+)"""".toRegex()
                match = fallbackRegex.find(html)?.groupValues?.get(1)
            }
            
            return match?.replace("\\/", "/")?.replace("\\u0026", "&")
        } catch (e: Exception) {
            Log.e("Extractor", "Dailymotion Error: ${e.message}")
        }
        return null
    }

    private suspend fun deepScanForVideo(url: String): String? {
        val doc = Jsoup.connect(url).userAgent(USER_AGENT).get()
        val html = doc.html()
        val videoRegex = """(https?://[^\s"']+\.(?:m3u8|mp4)[^\s"']*)""".toRegex()
        return videoRegex.find(html)?.value?.takeIf { !it.contains("thumbnail") }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
}
