package com.aku.anice.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.aku.anice.data.model.VideoTrack

@Composable
fun ResolutionPicker(
    tracks: List<VideoTrack>,
    onTrackSelected: (VideoTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Pilih Resolusi", modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn {
            item {
                ResolutionItem(
                    label = "Otomatis",
                    isSelected = tracks.none { it.isSelected },
                    onClick = { onTrackSelected(null) }
                )
            }
            items(tracks) { track ->
                ResolutionItem(
                    label = track.label,
                    isSelected = track.isSelected,
                    onClick = { onTrackSelected(track) }
                )
            }
        }
    }
}

@Composable
private fun ResolutionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

// Logic to extract tracks from ExoPlayer
fun getAvailableVideoTracks(player: Player): List<VideoTrack> {
    val videoTracks = mutableListOf<VideoTrack>()
    val tracks = player.currentTracks

    for (groupIndex in 0 until tracks.groups.size) {
        val group = tracks.groups[groupIndex]
        if (group.type != C.TRACK_TYPE_VIDEO) continue

        for (trackIndex in 0 until group.length) {
            if (!group.isTrackSupported(trackIndex)) continue
            
            val format = group.getTrackFormat(trackIndex)
            val label = if (format.height > 0) "${format.height}p" else "Unknown"
            
            videoTracks.add(
                VideoTrack(
                    id = format.id ?: trackIndex.toString(),
                    label = label,
                    width = format.width,
                    height = format.height,
                    format = format,
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    isSelected = group.isTrackSelected(trackIndex)
                )
            )
        }
    }
    return videoTracks.sortedByDescending { it.height }
}

// Logic to update resolution
fun updatePlayerResolution(player: Player, videoTrack: VideoTrack?) {
    val parametersBuilder = player.trackSelectionParameters.buildUpon()
    
    if (videoTrack == null) {
        // Set to Auto: Clear overrides and reset max constraints
        parametersBuilder
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSizeSd() // Optional: reset to default
    } else {
        // Set specific resolution
        val trackGroup = player.currentTracks.groups[videoTrack.groupIndex].mediaTrackGroup
        parametersBuilder.setOverrideForType(
            TrackSelectionOverride(trackGroup, videoTrack.trackIndex)
        )
    }
    
    player.trackSelectionParameters = parametersBuilder.build()
}
