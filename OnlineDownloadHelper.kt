package com.playmusic.app.online

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.DownloadRequest

object OnlineDownloadHelper {
    fun requestForTrack(
        context: Context,
        id: String,
        title: String,
        uri: Uri
    ) {
        val request = DownloadRequest.Builder(id, uri)
            .setData(title.toByteArray(Charsets.UTF_8))
            .setMimeType("audio/mpeg")
            .build()
        OfflineDownloadManager.add(context, request)
    }

    fun requestForMediaItem(
        context: Context,
        id: String,
        mediaItem: MediaItem
    ) {
        val uri = mediaItem.localConfiguration?.uri ?: return
        requestForTrack(context, id, mediaItem.mediaMetadata.title?.toString() ?: id, uri)
    }
}
