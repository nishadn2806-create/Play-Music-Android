package com.playmusic.app

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player

/**
 * Unified queue helper for local, online and downloaded MediaItems.
 * The same Media3 Player/MediaController playlist can contain all three.
 */
object UnifiedQueueManager {
    fun add(player: Player, item: MediaItem) {
        player.addMediaItem(item)
    }

    fun addNext(player: Player, item: MediaItem) {
        val index = (player.currentMediaItemIndex + 1).coerceAtLeast(0)
        player.addMediaItem(index, item)
    }

    fun remove(player: Player, index: Int) {
        if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
    }

    fun move(player: Player, from: Int, to: Int) {
        if (from in 0 until player.mediaItemCount && to in 0 until player.mediaItemCount) {
            player.moveMediaItem(from, to)
        }
    }

    fun clear(player: Player) {
        player.clearMediaItems()
    }

    fun snapshot(player: Player): List<MediaItem> =
        (0 until player.mediaItemCount).map(player::getMediaItem)

    fun build(
        id: String,
        uri: String,
        title: String,
        artist: String? = null,
        album: String? = null,
        artworkUri: String? = null
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
