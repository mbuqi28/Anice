package com.aku.anice.ui.home

data class FilterState(
    val status: String = "", // "", "ongoing", "completed"
    val order: String = "update" // "update", "latest", "popular", "title"
)
