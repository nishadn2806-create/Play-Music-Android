package com.playmusic.app

import android.content.Context
import androidx.room.*

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val position: Int)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(@PrimaryKey val key: Int = 1, val songId: Long, val position: Long)

@Dao
interface LibraryDao {
    @Query("SELECT songId FROM favorites") suspend fun favoriteIds(): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addFavorite(x: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE songId=:id") suspend fun removeFavorite(id: Long)

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") suspend fun playlists(): List<PlaylistEntity>
    @Insert suspend fun addPlaylist(x: PlaylistEntity): Long
    @Query("DELETE FROM playlists WHERE id=:id") suspend fun deletePlaylist(id: Long)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId=:p ORDER BY position")
    suspend fun playlistSongs(p: Long): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addPlaylistSong(x: PlaylistSongEntity)
    @Query("DELETE FROM playlist_songs WHERE playlistId=:p AND songId=:s")
    suspend fun removePlaylistSong(p: Long, s: Long)
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId=:p")
    suspend fun nextPosition(p: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveState(x: PlaybackStateEntity)
    @Query("SELECT * FROM playback_state WHERE key=1") suspend fun state(): PlaybackStateEntity?
}

@Database(entities = [FavoriteEntity::class, PlaylistEntity::class, PlaylistSongEntity::class, PlaybackStateEntity::class],
    version = 4, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun dao(): LibraryDao
    companion object {
        @Volatile private var instance: LibraryDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, LibraryDatabase::class.java, "play_music.db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
