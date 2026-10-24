package com.playmusic.app.online

/** Provider-neutral contract for a legal online music integration. */
interface OnlineMusicProvider {
    suspend fun search(query: String): List<OnlineTrack>
    suspend fun getTrack(id: String): OnlineTrack?
    suspend fun getAlbum(id: String): OnlineAlbum?
    suspend fun getArtist(id: String): OnlineArtist?
    suspend fun resolvePlayableItem(id: String): OnlinePlayableItem?
    suspend fun getTrending(tag: String? = null, limit: Int = 12): List<OnlineTrack>
}

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUri: String? = null,
    val durationMs: Long = 0L
)

data class OnlineAlbum(val id: String, val title: String, val artist: String, val artworkUri: String? = null)
data class OnlineArtist(val id: String, val name: String, val artworkUri: String? = null)
data class OnlinePlayableItem(
    val trackId: String,
    val playbackUri: String? = null,
    val providerSessionToken: String? = null
)
