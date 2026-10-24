package com.playmusic.app

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object MusicRepository {
    fun loadSongs(r: ContentResolver): List<Song> {
        val result = mutableListOf<Song>()
        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )
        r.query(base, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val i = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val t = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val a = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val al = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val d = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val aid = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val path = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) {
                val id = c.getLong(i)
                val raw = if (path >= 0) c.getString(path).orEmpty() else ""
                val folder = raw.substringBeforeLast(File.separator, "")
                    .substringAfterLast(File.separator).ifBlank { "Music" }
                result += Song(id,
                    c.getString(t).orEmpty().ifBlank { "Unknown title" },
                    c.getString(a).orEmpty().ifBlank { "Unknown artist" },
                    c.getString(al).orEmpty().ifBlank { "Unknown album" },
                    c.getLong(d), ContentUris.withAppendedId(base, id),
                    c.getLong(aid), folder)
            }
        }
        return result
    }

    fun artwork(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}
