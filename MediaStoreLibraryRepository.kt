package com.playmusic.app

import android.content.ContentResolver

/** MediaStore-backed browse model for Albums, Artists and Folders. */
object MediaStoreLibraryRepository {
    fun loadGroups(resolver: ContentResolver): List<LibraryGroup> =
        groupsFrom(MusicRepository.loadSongs(resolver))

    fun groupsFrom(songs: List<Song>): List<LibraryGroup> {
        val albums = songs.groupBy { it.album.trim().ifBlank { "Unknown album" } }
            .map { (name, tracks) -> LibraryGroup(
                key = "album:$name", title = name,
                subtitle = "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}",
                artworkUri = tracks.firstOrNull()?.let { MusicRepository.artwork(it.albumId) },
                tracks = tracks
            ) }.sortedBy { it.title.lowercase() }

        val artists = songs.groupBy { it.artist.trim().ifBlank { "Unknown artist" } }
            .map { (name, tracks) -> LibraryGroup(
                key = "artist:$name", title = name,
                subtitle = "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}",
                tracks = tracks
            ) }.sortedBy { it.title.lowercase() }

        val folders = songs.groupBy { it.folder.trim().ifBlank { "Music" } }
            .map { (name, tracks) -> LibraryGroup(
                key = "folder:$name", title = name,
                subtitle = "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}",
                tracks = tracks
            ) }.sortedBy { it.title.lowercase() }

        return albums + artists + folders
    }
}
