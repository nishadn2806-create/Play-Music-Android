package com.playmusic.app.online

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-first online library.
 *
 * Favorites and playlists are kept locally so the feature works even when
 * the user has not connected a provider account. Provider synchronization
 * can be added later without making local playback dependent on the network.
 */
class OnlineLibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("online_library", Context.MODE_PRIVATE)

    fun isFavorite(trackId: String): Boolean =
        favorites().contains(trackId)

    fun setFavorite(trackId: String, favorite: Boolean) {
        val set = favorites().toMutableSet()
        if (favorite) set.add(trackId) else set.remove(trackId)
        prefs.edit().putStringSet("favorites", set).apply()
    }

    fun playlistNames(): List<String> =
        prefs.getStringSet("playlists", emptySet())!!.toList().sorted()

    fun createPlaylist(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        val set = prefs.getStringSet("playlists", emptySet())!!.toMutableSet()
        val changed = set.add(clean)
        prefs.edit().putStringSet("playlists", set).apply()
        return changed
    }

    fun addToPlaylist(name: String, trackId: String) {
        val key = "playlist_${name.trim()}"
        val set = prefs.getStringSet(key, emptySet())!!.toMutableSet()
        set.add(trackId)
        prefs.edit().putStringSet(key, set).apply()
    }

    fun playlistTrackIds(name: String): List<String> =
        prefs.getStringSet("playlist_${name.trim()}", emptySet())!!.toList()

    private fun favorites(): Set<String> =
        prefs.getStringSet("favorites", emptySet()) ?: emptySet()
}
