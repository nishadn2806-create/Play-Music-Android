package com.playmusic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem

/**
 * Artwork surface driven by MediaItem.MediaMetadata.artworkUri.
 *
 * The URI is exposed to the UI without changing the playback architecture.
 * A Coil/Glide image loader can be plugged in by the app later; the fallback
 * remains deterministic and uses the Play Music brand.
 */
@Composable
fun MediaArtwork(
    mediaItem: MediaItem?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val artworkUri = mediaItem?.mediaMetadata?.artworkUri
    if (artworkUri != null) {
        // URI-ready surface. The actual image loader can consume this URI.
        // Keeping this dependency-free avoids adding another network/image SDK.
        ArtworkFallback(modifier, mediaItem)
    } else {
        ArtworkFallback(modifier, mediaItem)
    }
}

@Composable
private fun ArtworkFallback(modifier: Modifier, mediaItem: MediaItem?) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFEDE7FF)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♫", color = Color(0xFF6D28D9),
                style = MaterialTheme.typography.displayLarge)
            val title = mediaItem?.mediaMetadata?.title?.toString().orEmpty()
            if (title.isNotBlank()) {
                Text(
                    title.take(24),
                    color = Color(0xFF2E1065),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
