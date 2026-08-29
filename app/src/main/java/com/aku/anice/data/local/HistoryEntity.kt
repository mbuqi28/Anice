package com.aku.anice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val detailUrl: String,
    val title: String,
    val thumbUrl: String,
    val lastEpisodeTitle: String,
    val lastEpisodeUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
