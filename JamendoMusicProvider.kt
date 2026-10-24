package com.playmusic.app.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * Jamendo API v3 provider.
 *
 * The client id is injected through BuildConfig and is intentionally not
 * hard-coded in source. Read-only catalog/search and stream resolution are
 * supported here. OAuth2 write operations are deliberately not included yet.
 */
class JamendoMusicProvider(
    private val clientId: String
) : OnlineMusicProvider {

    private val base = "https://api.jamendo.com/v3.0"

    override suspend fun search(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$base/tracks/?client_id=$clientId&format=json&limit=30&search=$encoded&audioformat=mp32"
        val json = get(url)
        val results = json.optJSONArray("results") ?: return@withContext emptyList()

        buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                add(item.toTrack())
            }
        }
    }

    override suspend fun getTrack(id: String): OnlineTrack? = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext null
        val url = "$base/tracks/?client_id=$clientId&format=json&id=${enc(id)}&audioformat=mp32"
        val results = get(url).optJSONArray("results") ?: return@withContext null
        results.optJSONObject(0)?.toTrack()
    }

    override suspend fun getAlbum(id: String): OnlineAlbum? = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext null
        val url = "$base/albums/?client_id=$clientId&format=json&id=${enc(id)}"
        val item = get(url).optJSONArray("results")?.optJSONObject(0) ?: return@withContext null
        OnlineAlbum(
            id = item.optString("id"),
            title = item.optString("name"),
            artist = item.optString("artist_name"),
            artworkUri = item.optString("image").takeIf { it.isNotBlank() }
                ?: item.optString("album_image").takeIf { it.isNotBlank() }
        )
    }

    override suspend fun getArtist(id: String): OnlineArtist? = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext null
        val url = "$base/artists/?client_id=$clientId&format=json&id=${enc(id)}"
        val item = get(url).optJSONArray("results")?.optJSONObject(0) ?: return@withContext null
        OnlineArtist(
            id = item.optString("id"),
            name = item.optString("name"),
            artworkUri = item.optString("image").takeIf { it.isNotBlank() }
        )
    }


    override suspend fun getTrending(tag: String?, limit: Int): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext emptyList()
        val safeLimit = limit.coerceIn(1, 50)
        val tagPart = tag?.takeIf { it.isNotBlank() }?.let { "&tag=${enc(it)}" } ?: ""
        val chartUrl = "$base/charts/track/?client_id=$clientId&format=json&limit=$safeLimit&order=trending$tagPart"
        val chartResults = get(chartUrl).optJSONArray("results") ?: return@withContext emptyList()

        val ids = buildList {
            for (i in 0 until chartResults.length()) {
                val id = chartResults.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isNotBlank()) add(id)
            }
        }
        if (ids.isEmpty()) return@withContext emptyList()

        val trackUrl = "$base/tracks/?client_id=$clientId&format=json&limit=${ids.size}&id=${ids.joinToString("+")}&audioformat=mp32&imagesize=150"
        val tracks = get(trackUrl).optJSONArray("results") ?: return@withContext emptyList()
        val byId = buildMap {
            for (i in 0 until tracks.length()) {
                val item = tracks.optJSONObject(i) ?: continue
                put(item.optString("id"), item.toTrack())
            }
        }
        ids.mapNotNull { byId[it] }
    }

    override suspend fun resolvePlayableItem(id: String): OnlinePlayableItem? = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext null
        val url = "$base/tracks/file/?client_id=$clientId&id=${enc(id)}&action=stream&audioformat=mp32"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = false
        }
        try {
            val location = connection.getHeaderField("Location")
                ?: connection.url?.toString()
            if (connection.responseCode in 200..399 && !location.isNullOrBlank()) {
                OnlinePlayableItem(trackId = id, playbackUri = location)
            } else null
        } finally {
            connection.disconnect()
        }
    }

    private fun get(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Online provider HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toTrack(): OnlineTrack {
        val artwork = optString("image").takeIf { it.isNotBlank() }
            ?: optString("album_image").takeIf { it.isNotBlank() }
        return OnlineTrack(
            id = optString("id"),
            title = optString("name"),
            artist = optString("artist_name"),
            album = optString("album_name").takeIf { it.isNotBlank() },
            artworkUri = artwork,
            durationMs = optLong("duration", 0L) * 1000L
        ,
            downloadAllowed = optBoolean("audiodownload_allowed", false))
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
