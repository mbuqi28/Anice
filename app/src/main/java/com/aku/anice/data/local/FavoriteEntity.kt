package com.aku.anice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val detailUrl: String,
    val title: String,
    val thumbUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
