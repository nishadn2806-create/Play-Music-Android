package com.playmusic.app

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import com.playmusic.app.online.OnlineLibraryManager
import com.playmusic.app.online.OnlineTrack
import com.playmusic.app.online.OnlineDownloadAction

/**
 * Shared action row for online tracks.
 * Download is shown only when the provider explicitly allows it.
 */
@Composable
fun OnlineTrackActions(
    context: Context,
    track: OnlineTrack,
    library: OnlineLibraryManager,
    onQueue: () -> Unit,
    onDownloadStarted: () -> Unit = {}
) {
    var favorite by remember(track.id) { mutableStateOf(library.isFavorite(track.id)) }

    IconButton(onClick = onQueue) {
        Icon(Icons.Rounded.QueueMusic, "Add to queue")
    }
    IconButton(onClick = {
        favorite = if (favorite) {
            library.removeFavorite(track.id)
            false
        } else {
            library.addFavorite(track.id)
            true
        }
    }) {
        Icon(
            if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (favorite) "Remove favorite" else "Favorite"
        )
    }
    if (track.downloadAllowed) {
        IconButton(onClick = {
            val started = OnlineDownloadAction.download(
                context, track.id, track.title, track.audioUrl
            )
            if (started) onDownloadStarted()
        }) {
            Icon(Icons.Rounded.Download, "Download for offline")
        }
    }
}
