package com.playmusic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem

@Composable
fun ArtworkPlaceholder(
    mediaItem: MediaItem?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFEDE7FF)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("♫", color = Color(0xFF6D28D9), style = androidx.compose.material3.MaterialTheme.typography.displayLarge)
    }
}
