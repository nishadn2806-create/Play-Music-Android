package com.playmusic.app.online

import android.content.Context
import android.net.Uri

object OnlineDownloadAction {
    /**
     * Returns false when a provider has no direct downloadable URI.
     * Callers should only show a Download button when this returns true.
     */
    fun canDownload(audioUrl: String?): Boolean =
        !audioUrl.isNullOrBlank() &&
        (audioUrl.startsWith("https://") || audioUrl.startsWith("http://"))

    fun download(
        context: Context,
        id: String,
        title: String,
        audioUrl: String?
    ): Boolean {
        if (!canDownload(audioUrl)) return false
        OnlineDownloadHelper.requestForTrack(
            context,
            id,
            title,
            Uri.parse(audioUrl)
        )
        return true
    }
}
