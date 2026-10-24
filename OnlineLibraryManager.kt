package com.playmusic.app.online

import android.content.Context

/**
 * Coordinates local-first favorites/playlists.
 *
 * Provider-side sync intentionally remains opt-in and is not attempted without
 * an authenticated provider session.
 */
class OnlineLibraryManager(context: Context) {
    private val store = OnlineLibraryStore(context)

    fun toggleFavorite(trackId: String): Boolean {
        val next = !store.isFavorite(trackId)
        store.setFavorite(trackId, next)
        return next
    }

    fun isFavorite(trackId: String): Boolean = store.isFavorite(trackId)

    fun createPlaylist(name: String): Boolean = store.createPlaylist(name)

    fun addToPlaylist(name: String, trackId: String) =
        store.addToPlaylist(name, trackId)

    fun playlists(): List<String> = store.playlistNames()
}
