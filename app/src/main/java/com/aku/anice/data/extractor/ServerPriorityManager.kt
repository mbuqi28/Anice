package com.aku.anice.data.extractor

import com.aku.anice.data.model.VideoSource
import java.util.LinkedList
import java.util.Queue

object ServerPriorityManager {

    /**
     * Prioritizes servers based on requirements:
     * 1. Hardsub Indonesia + Dailymotion
     * 2. Hardsub Indonesia + Ok.ru
     * 3. Other Hardsub Indonesia (StreamHide, etc.)
     * 4. All Sub (JW Player, Dailymotion, Ok.ru)
     * 5. Fallback: English Sub / Others
     */
    fun getPriorityQueue(sources: List<VideoSource>): Queue<VideoSource> {
        val sortedList = sources.sortedWith(compareByDescending<VideoSource> { source ->
            val name = source.serverName.lowercase()
            when {
                name.contains("hardsub") && name.contains("dailymotion") -> 100
                name.contains("hardsub") && (name.contains("ok.ru") || name.contains("odnoklassniki")) -> 90
                name.contains("hardsub") -> 80
                name.contains("all sub") || name.contains("multi") -> 70
                name.contains("streamhide") || name.contains("streamsb") || name.contains("dood") -> 60
                else -> 50
            }
        })
        
        return LinkedList(sortedList)
    }
}
