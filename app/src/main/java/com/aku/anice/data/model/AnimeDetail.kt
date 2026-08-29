package com.aku.anice.data.model

data class AnimeDetail(
    val title: String,
    val episodes: List<Episode>
)

data class VideoSource(
    val serverName: String,
    val url: String
)
