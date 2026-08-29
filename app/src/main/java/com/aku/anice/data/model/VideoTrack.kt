package com.aku.anice.data.model

import androidx.media3.common.Format

data class VideoTrack(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val format: Format,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)
