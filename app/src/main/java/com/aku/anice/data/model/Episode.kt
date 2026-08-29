package com.aku.anice.data.model

data class Episode(
    val id: String,
    val number: String,
    val title: String,
    val url: String,
    val date: String = "" // Tambahkan field tanggal
)
