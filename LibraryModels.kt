package com.playmusic.app

import android.net.Uri

data class LibraryGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUri: Uri? = null,
    val tracks: List<Song>
)
