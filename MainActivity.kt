package com.playmusic.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.playmusic.app.online.JamendoMusicProvider
import com.playmusic.app.online.OnlineTrack

private val Violet = Color(0xFF6D28D9)
private val Deep = Color(0xFF2E1065)
private val Lav = Color(0xFFC4B5FD)
private val Light = Color(0xFFEDE7FF)

class MainActivity : ComponentActivity() {
    private lateinit var db: LibraryDatabase
    private lateinit var settings: SettingsStore
    private lateinit var future: ListenableFuture<MediaController>
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var favorites by mutableStateOf<Set<Long>>(emptySet())
    private var playlists by mutableStateOf<List<PlaylistEntity>>(emptyList())
    private var dark by mutableStateOf(false)
    private var detail by mutableStateOf<String?>(null)
    private var restoredOnce = false
    private val onlineProvider by lazy { JamendoMusicProvider(BuildConfig.JAMENDO_CLIENT_ID) }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) refresh()
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        db = LibraryDatabase.get(this)
        settings = SettingsStore(this)
        dark = settings.darkMode
        lifecycleScope.launch {
            favorites = db.dao().favoriteIds().toSet()
            playlists = db.dao().playlists()
        }
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED) refresh()
        else permission.launch(p)

        future = MediaController.Builder(this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()

        setContent {
            App(songs, favorites, playlists, dark, detail,
                { dark = it; settings.darkMode = it }, { detail = it }, ::refresh,
                ::toggleFavorite, ::createPlaylist, ::deletePlaylist, ::playlistSongs,
                ::addToPlaylist, ::removeFromPlaylist, ::play, ::sleep, future, isOnlineAvailable(this), ::searchOnline, ::playOnline, ::queueOnline, ::discoverOnline)
        }
    }

    private fun refresh() {
        songs = MusicRepository.loadSongs(contentResolver)
        restorePlaybackIfReady()
    }

    private fun restorePlaybackIfReady() {
        if (restoredOnce || songs.isEmpty() || !future.isDone || future.isCancelled) return
        restoredOnce = true
        lifecycleScope.launch {
            val saved = db.dao().state() ?: return@launch
            val source = songs
            val index = source.indexOfFirst { it.id == saved.songId }
            if (index >= 0) {
                val c = future.get()
                c.setMediaItems(mediaItems(source), index, saved.position)
                c.prepare()
                // Do not auto-play: restore position and let the user press Play.
            }
        }
    }

    private fun toggleFavorite(id: Long) = lifecycleScope.launch {
        if (id in favorites) { db.dao().removeFavorite(id); favorites -= id }
        else { db.dao().addFavorite(FavoriteEntity(id)); favorites += id }
    }

    private fun createPlaylist(name: String) = lifecycleScope.launch {
        if (name.isBlank()) return@launch
        db.dao().addPlaylist(PlaylistEntity(name = name))
        playlists = db.dao().playlists()
    }

    private fun deletePlaylist(id: Long) = lifecycleScope.launch {
        db.dao().deletePlaylist(id)
        playlists = db.dao().playlists()
    }

    private fun playlistSongs(id: Long, result: (List<Song>) -> Unit) = lifecycleScope.launch {
        val ids = db.dao().playlistSongs(id).toSet()
        result(ids.mapNotNull { x -> songs.find { it.id == x } })
    }

    private fun addToPlaylist(pid: Long, sid: Long) = lifecycleScope.launch {
        val existing = db.dao().playlistSongs(pid)
        if (sid !in existing) db.dao().addPlaylistSong(
            PlaylistSongEntity(pid, sid, db.dao().nextPosition(pid))
        )
    }

    private fun removeFromPlaylist(pid: Long, sid: Long) = lifecycleScope.launch {
        db.dao().removePlaylistSong(pid, sid)
    }

    private fun mediaItems(source: List<Song>) = source.map {
        MediaItem.Builder().setMediaId(it.id.toString()).setUri(it.uri).setMediaMetadata(
            MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album)
                .setArtworkUri(MusicRepository.artwork(it.albumId)).build()
        ).build()
    }

    private fun play(song: Song) {
        future.addListener({
            val c = future.get()
            val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            c.setMediaItems(mediaItems(songs), index, 0L)
            c.prepare(); c.play()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun searchOnline(query: String, result: (List<OnlineTrack>, String?) -> Unit) {
        lifecycleScope.launch {
            if (BuildConfig.JAMENDO_CLIENT_ID.isBlank()) {
                result(emptyList(), "Add jamendoClientId to Gradle properties before using Online Music.")
                return@launch
            }
            try {
                val tracks = onlineProvider.search(query)
                result(tracks, null)
            } catch (e: Exception) {
                result(emptyList(), "Online search failed. Check your internet connection.")
            }
        }
    }


    private fun discoverOnline(tag: String?, result: (List<OnlineTrack>, String?) -> Unit) {
        lifecycleScope.launch {
            if (BuildConfig.JAMENDO_CLIENT_ID.isBlank()) {
                result(emptyList(), "Add jamendoClientId to Gradle properties before using Online Music.")
                return@launch
            }
            try {
                result(onlineProvider.getTrending(tag, 12), null)
            } catch (_: Exception) {
                result(emptyList(), "Online discovery failed. Check your internet connection.")
            }
        }
    }

    private fun playOnline(track: OnlineTrack) {
        lifecycleScope.launch {
            try {
                val playable = onlineProvider.resolvePlayableItem(track.id)
                val uri = playable?.playbackUri ?: return@launch
                val item = MediaItem.Builder()
                    .setMediaId("jamendo:${track.id}")
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .setArtworkUri(track.artworkUri?.let { android.net.Uri.parse(it) })
                            .build()
                    ).build()
                val c = future.get()
                c.setMediaItem(item)
                c.prepare()
                c.play()
            } catch (_: Exception) {
                // Keep the offline player alive if online playback fails.
            }
        }
    }

    private fun queueOnline(track: OnlineTrack) {
        lifecycleScope.launch {
            try {
                val playable = onlineProvider.resolvePlayableItem(track.id)
                val uri = playable?.playbackUri ?: return@launch
                val item = MediaItem.Builder()
                    .setMediaId("jamendo:${track.id}")
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .setArtworkUri(track.artworkUri?.let { android.net.Uri.parse(it) })
                            .build()
                    ).build()
                future.get().addMediaItem(item)
            } catch (_: Exception) {
                // Queue failure must not affect existing playback.
            }
        }
    }

    private fun sleep(minutes: Int) {
        startService(Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_SLEEP
            putExtra(PlaybackService.EXTRA_MINUTES, minutes)
        })
    }

    override fun onStop() {
        super.onStop()
        if (::future.isInitialized && future.isDone && !future.isCancelled) {
            val c = future.get()
            val id = c.currentMediaItem?.mediaId?.toLongOrNull()
            if (id != null) lifecycleScope.launch {
                db.dao().saveState(PlaybackStateEntity(songId = id, position = c.currentPosition))
            }
        }
    }

    override fun onDestroy() {
        if (::future.isInitialized) MediaController.releaseFuture(future)
        super.onDestroy()
    }
}

@Composable
private fun App(
    songs: List<Song>, favorites: Set<Long>, playlists: List<PlaylistEntity>, dark: Boolean,
    detail: String?, setDark: (Boolean) -> Unit, setDetail: (String?) -> Unit,
    refresh: () -> Unit, toggle: (Long) -> Unit, create: (String) -> Unit,
    delete: (Long) -> Unit, loadPlaylist: (Long, (List<Song>) -> Unit) -> Unit,
    add: (Long, Long) -> Unit, remove: (Long, Long) -> Unit,
    play: (Song) -> Unit, sleep: (Int) -> Unit, future: ListenableFuture<MediaController>, onlineAvailable: Boolean,
    searchOnline: (String, (List<OnlineTrack>, String?) -> Unit) -> Unit,
    playOnline: (OnlineTrack) -> Unit,
    queueOnline: (OnlineTrack) -> Unit,
    discoverOnline: (String?, (List<OnlineTrack>, String?) -> Unit) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Lav)
    else lightColorScheme(primary = Violet, background = Light)) {
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Rounded.Home, null) }, label = { Text("Home") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Rounded.MusicNote, null) }, label = { Text("Songs") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Rounded.Cloud, null) }, label = { Text("Online") })
                NavigationBarItem(tab == 3, { tab = 3 }, icon = { Icon(Icons.Rounded.LibraryMusic, null) }, label = { Text("Library") })
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                if (detail != null) {
                    Detail(detail, songs, playlists, play, loadPlaylist, add, remove, delete, setDetail)
                } else when (tab) {
                    0 -> Home(songs.size, refresh) { settings = true }
                    1 -> Songs(songs, query, { query = it }, favorites, toggle, play)
                    2 -> OnlineMusicScreen(onlineAvailable, searchOnline, playOnline, queueOnline, discoverOnline)
                    else -> LibraryBrowseScreen(songs, setDetail, ::refresh)
                }
            }
            Mini(future) { now = true }
            if (now) NowPlaying(future) { now = false }
            if (settings) Settings(dark, setDark, sleep) { settings = false }
        }
    }
}

@Composable
private fun OnlineMusicScreen(
    available: Boolean,
    searchOnline: (String, (List<OnlineTrack>, String?) -> Unit) -> Unit,
    playOnline: (OnlineTrack) -> Unit,
    queueOnline: (OnlineTrack) -> Unit,
    discoverOnline: (String?, (List<OnlineTrack>, String?) -> Unit) -> Unit
) {
    val genres = listOf("All", "Pop", "Rock", "Electronic", "Jazz", "Hip Hop", "Classical", "Chillout", "Ambient", "Indie")
    var selectedGenre by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var trending by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loadingDiscover by remember { mutableStateOf(false) }

    fun selectedTag(): String? = selectedGenre.takeUnless { it == "All" }?.lowercase()?.replace(" ", "")

    LaunchedEffect(available, selectedGenre) {
        if (!available) {
            trending = emptyList()
            return@LaunchedEffect
        }
        loadingDiscover = true
        error = null
        discoverOnline(selectedTag()) { tracks, message ->
            trending = tracks
            error = message
            loadingDiscover = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Discover",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Deep
        )
        Text("Find music beyond your local library.", color = Violet)

        Surface(
            Modifier.fillMaxWidth(),
            color = if (available) Violet else Deep,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Cloud, null, Modifier.size(42.dp), tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (available) "Online discovery ready" else "Offline mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (available) "Trending tracks and genre discovery"
                        else "Your local music continues to work",
                        color = Lav
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = available,
            placeholder = { Text("Search online music") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                IconButton(
                    enabled = available && query.isNotBlank() && !loading,
                    onClick = {
                        loading = true
                        error = null
                        searchOnline(query) { tracks, message ->
                            results = tracks
                            error = message
                            loading = false
                        }
                    }
                ) { Icon(Icons.Rounded.Search, "Search") }
            }
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(genres) { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre) }
                )
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (loadingDiscover) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (results.isNotEmpty()) {
            Text("Search Results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OnlineTrackList(results, playOnline, queueOnline)
        } else if (available) {
            Text(
                if (selectedGenre == "All") "Trending Now" else "$selectedGenre • Trending",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (trending.isEmpty() && !loadingDiscover && error == null) {
                Text("No tracks found for this category.", color = Color.Gray)
            } else {
                OnlineTrackList(trending, playOnline, queueOnline)
            }
        } else {
            Text(
                "No internet connection. Offline library remains fully available.",
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun OnlineTrackList(
    tracks: List<OnlineTrack>,
    playOnline: (OnlineTrack) -> Unit,
    queueOnline: (OnlineTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            ListItem(
                modifier = Modifier.clickable { playOnline(track) },
                headlineContent = { Text(track.title, maxLines = 1) },
                supportingContent = {
                    Text(
                        listOfNotNull(track.artist, track.album).joinToString(" • "),
                        maxLines = 1
                    )
                },
                leadingContent = {
                    Icon(Icons.Rounded.MusicNote, null, tint = Violet)
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { queueOnline(track) }) {
                            Icon(Icons.Rounded.QueueMusic, "Add to queue")
                        }
                        IconButton(onClick = { playOnline(track) }) {
                            Icon(Icons.Rounded.PlayArrow, "Play")
                        }
                    }
                }
            )
        }
    }
}

private fun isOnlineAvailable(context: android.content.Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable private fun Home(count: Int, refresh: () -> Unit, settings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Play Music", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Deep)
        Text("Your music. Your way.", color = Violet)
        Surface(Modifier.fillMaxWidth(), color = Violet, shape = MaterialTheme.shapes.extraLarge) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Headphones, null, Modifier.size(52.dp), tint = Color.White)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Offline music", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("$count songs", color = Lav)
                }
            }
        }
        Button(refresh) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(5.dp)); Text("Refresh library") }
        OutlinedButton(settings) { Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(5.dp)); Text("Settings") }
    }
}

@Composable private fun Songs(
    songs: List<Song>, query: String, setQuery: (String) -> Unit, favorites: Set<Long>,
    toggle: (Long) -> Unit, play: (Song) -> Unit
) {
    val list = songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Songs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Deep)
        OutlinedTextField(query, setQuery, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search title, artist or album") }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
        LazyColumn {
            itemsIndexed(list, key = { _, s -> s.id }) { _, song ->
                Row(Modifier.fillMaxWidth().clickable { play(song) }.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(36.dp), tint = Violet)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = Deep, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${song.artist} • ${song.album}", color = Color.Gray, maxLines = 1)
                    }
                    IconButton({ toggle(song.id) }) {
                        Icon(if (song.id in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = Violet)
                    }
                }
            }
        }
    }
}

@Composable private fun Library(
    songs: List<Song>, favorites: Set<Long>, playlists: List<PlaylistEntity>,
    toggle: (Long) -> Unit, create: (String) -> Unit, detail: (String) -> Unit, settings: () -> Unit
) {
    var new by remember { mutableStateOf(false) }
    val albums = songs.groupBy { it.album }.entries.sortedBy { it.key.lowercase() }
    val artists = songs.groupBy { it.artist }.keys.sorted()
    val folders = songs.groupBy { it.folder }.keys.sorted()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Deep)
            Text("Favorites • ${favorites.size} songs", color = Violet)
            Button({ new = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text("New playlist") }
        }
        item { Text("Albums", fontWeight = FontWeight.Bold, color = Deep) }
        itemsIndexed(albums, key = { _, e -> e.key }) { _, e -> RowItem(Icons.Rounded.Album, e.key, "${e.value.size} songs") { detail("album:${e.key}") } }
        item { Text("Artists", fontWeight = FontWeight.Bold, color = Deep) }
        itemsIndexed(artists, key = { _, a -> a }) { _, a -> RowItem(Icons.Rounded.Person, a, "${songs.count { it.artist == a }} songs") { detail("artist:$a") } }
        item { Text("Folders", fontWeight = FontWeight.Bold, color = Deep) }
        itemsIndexed(folders, key = { _, f -> f }) { _, f -> RowItem(Icons.Rounded.Folder, f, "${songs.count { it.folder == f }} songs") { detail("folder:$f") } }
        item { Text("Playlists", fontWeight = FontWeight.Bold, color = Deep) }
        items(playlists, key = { it.id }) { p -> RowItem(Icons.Rounded.QueueMusic, p.name, "Edit playlist") { detail("playlist:${p.id}") } }
        item { OutlinedButton(settings) { Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(5.dp)); Text("Settings") } }
    }
    if (new) NewPlaylist({ create(it); new = false }) { new = false }
}

@Composable private fun RowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = click).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Violet, Modifier.size(30.dp)); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, color = Deep, fontWeight = FontWeight.SemiBold); Text(sub, color = Color.Gray) }
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.Gray)
    }
}

@Composable private fun Detail(
    key: String, songs: List<Song>, playlists: List<PlaylistEntity>, play: (Song) -> Unit,
    load: (Long, (List<Song>) -> Unit) -> Unit, add: (Long, Long) -> Unit,
    remove: (Long, Long) -> Unit, delete: (Long) -> Unit, close: (String?) -> Unit
) {
    val type = key.substringBefore(":"); val value = key.substringAfter(":")
    var items by remember(key) { mutableStateOf<List<Song>>(emptyList()) }
    var addOpen by remember { mutableStateOf(false) }
    LaunchedEffect(key) { if (type == "playlist") load(value.toLongOrNull() ?: -1L) { items = it } }
    val list = when (type) {
        "album" -> songs.filter { it.album == value }
        "artist" -> songs.filter { it.artist == value }
        "folder" -> songs.filter { it.folder == value }
        "playlist" -> items
        else -> emptyList()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton({ close(null) }) { Text("‹ Back") }
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Deep)
        if (type == "playlist") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ addOpen = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("Add songs") }
                OutlinedButton({ delete(value.toLongOrNull() ?: -1L); close(null) }) { Text("Delete") }
            }
        }
        Text("${list.size} songs", color = Violet)
        LazyColumn {
            itemsIndexed(list, key = { _, s -> s.id }) { _, song ->
                Row(Modifier.fillMaxWidth().padding(8.dp).clickable { play(song) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Violet, Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                        Text(song.title, color = Deep); Text(song.artist, color = Color.Gray)
                    }
                    if (type == "playlist") IconButton({
                        remove(value.toLong(), song.id); items = items.filterNot { it.id == song.id }
                    }) { Icon(Icons.Rounded.RemoveCircleOutline, null, tint = Violet) }
                }
            }
        }
    }
    if (addOpen && type == "playlist") AddSongsDialog(songs, items.map { it.id }.toSet(), { id ->
        add(value.toLong(), id); songs.find { it.id == id }?.let { items += it }
    }, { addOpen = false })
}

@Composable private fun AddSongsDialog(songs: List<Song>, selected: Set<Long>, add: (Long) -> Unit, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text("Add songs") },
        text = { LazyColumn(Modifier.heightIn(max = 420.dp)) {
            itemsIndexed(songs, key = { _, s -> s.id }) { _, s ->
                Row(Modifier.fillMaxWidth().clickable { if (s.id !in selected) add(s.id) }.padding(7.dp)) {
                    Icon(if (s.id in selected) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline, null, tint = Violet)
                    Spacer(Modifier.width(8.dp)); Text(s.title, color = Deep, maxLines = 1)
                }
            }
        }},
        confirmButton = { TextButton(close) { Text("Done") } })
}

@Composable private fun QueueScreen(future: ListenableFuture<MediaController>, close: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val c = if (future.isDone && !future.isCancelled) future.get() else null
    val items = c?.let { (0 until it.mediaItemCount).map { i -> it.getMediaItemAt(i) } } ?: emptyList()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Queue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Deep, Modifier.weight(1f))
            TextButton(close) { Text("Close") }
        }
        LazyColumn {
            itemsIndexed(items, key = { i, x -> "${x.mediaId}-$i-$tick" }) { i, item ->
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", Modifier.width(30.dp), color = Violet)
                    Text(item.mediaMetadata.title?.toString() ?: "Unknown", Modifier.weight(1f), color = Deep)
                    IconButton(enabled = i > 0, onClick = {
                        c?.moveMediaItem(i, i - 1); tick++
                    }) { Icon(Icons.Rounded.KeyboardArrowUp, null) }
                    IconButton(enabled = i < items.lastIndex, onClick = {
                        c?.moveMediaItem(i, i + 1); tick++
                    }) { Icon(Icons.Rounded.KeyboardArrowDown, null) }
                    IconButton(onClick = { c?.removeMediaItem(i); tick++ }) {
                        Icon(Icons.Rounded.Close, null, tint = Violet)
                    }
                }
            }
        }
    }
}

@Composable private fun Mini(future: ListenableFuture<MediaController>, open: () -> Unit) {
    var title by remember { mutableStateOf("") }; var playing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) {
        if (future.isDone && !future.isCancelled) {
            val c = future.get(); title = c.mediaMetadata.title?.toString().orEmpty(); playing = c.isPlaying
        }
        delay(800)
    }}
    if (title.isNotBlank()) Surface(Modifier.fillMaxWidth().clickable(open), color = Deep) {
        Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Headphones, null, tint = Lav, Modifier.size(30.dp)); Spacer(Modifier.width(8.dp))
            Text(text = title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton({ if (playing) future.get().pause() else future.get().play() }) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White)
            }
        }
    }
}

@Composable private fun NowPlaying(future: ListenableFuture<MediaController>, close: () -> Unit) {
    var pos by remember { mutableLongStateOf(0L) }; var dur by remember { mutableLongStateOf(1L) }
    var title by remember { mutableStateOf("Now Playing") }; var artist by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) {
        if (future.isDone && !future.isCancelled) {
            val c = future.get(); pos = c.currentPosition; dur = c.duration.takeIf { it > 0 } ?: 1L
            title = c.mediaMetadata.title?.toString() ?: "Now Playing"; artist = c.mediaMetadata.artist?.toString().orEmpty()
            playing = c.isPlaying
        }; delay(500)
    }}
    Surface(Modifier.fillMaxSize(), color = Light) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Headphones, null, Modifier.size(130.dp), tint = Violet)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Deep, fontWeight = FontWeight.Bold)
            Text(artist, color = Violet)
            Slider(pos.toFloat().coerceIn(0f, dur.toFloat()), { pos = it.toLong() },
                Modifier.fillMaxWidth(), onValueChangeFinished = { future.get().seekTo(pos) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton({ future.get().seekToPreviousMediaItem() }) { Icon(Icons.Rounded.SkipPrevious, null) }
                FilledIconButton({ if (playing) future.get().pause() else future.get().play() }) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                }
                IconButton({ future.get().seekToNextMediaItem() }) { Icon(Icons.Rounded.SkipNext, null) }
            }
            TextButton(close) { Text("Close") }
        }
    }
}

@Composable private fun Settings(dark: Boolean, setDark: (Boolean) -> Unit, sleep: (Int) -> Unit, close: () -> Unit) {
    var sleepOpen by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = close, title = { Text("Settings") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Dark mode", Modifier.weight(1f)); Switch(dark, setDark) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Sleep timer", Modifier.weight(1f)); TextButton({ sleepOpen = true }) { Text("Set") } }
            Text("Background playback and lock-screen controls use Media3.", color = Color.Gray)
            Text("Play Music V3.1 RC", color = Violet, fontWeight = FontWeight.SemiBold)
        }
    }, confirmButton = { TextButton(close) { Text("Done") } })
    if (sleepOpen) AlertDialog(onDismissRequest = { sleepOpen = false }, title = { Text("Sleep timer") },
        text = { Column { listOf(15,30,45,60).forEach { m -> TextButton({ sleep(m); sleepOpen=false }) { Text("$m minutes") } }; TextButton({ sleep(0); sleepOpen=false }) { Text("Off") } } },
        confirmButton = {})
}

@Composable private fun NewPlaylist(save: (String) -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = close, title = { Text("Create playlist") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Playlist name") }) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { save(name.trim()) }) { Text("Create") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}
