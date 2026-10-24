package com.playmusic.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.playmusic.app.online.DownloadRepository
import kotlin.math.roundToInt

@Composable
fun DownloadsScreen(
    repository: DownloadRepository,
    onPlayDownload: (Download) -> Unit
) {
    var downloads by remember { mutableStateOf(repository.all()) }
    var paused by remember { mutableStateOf(false) }

    DisposableEffect(repository) {
        val listener = object : androidx.media3.exoplayer.offline.DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: androidx.media3.exoplayer.offline.DownloadManager,
                download: Download,
                finalException: Exception?
            ) { downloads = repository.all() }

            override fun onDownloadRemoved(
                downloadManager: androidx.media3.exoplayer.offline.DownloadManager,
                download: Download
            ) { downloads = repository.all() }
        }
        repository.addListener(listener)
        onDispose { repository.removeListener(listener) }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "My Downloads",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Download, contentDescription = null)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (paused) repository.resumeAll() else repository.pauseAll()
                paused = !paused
            }) {
                Icon(
                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null
                )
                Text(if (paused) " Resume" else " Pause")
            }
        }

        if (downloads.isEmpty()) {
            Text("No downloaded songs yet.", color = androidx.compose.ui.graphics.Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(downloads, key = { it.request.id }) { download ->
                    val percent = download.percent.takeIf { it >= 0f } ?: 0f
                    ListItem(
                        headlineContent = {
                            Text(
                                String(download.request.data, Charsets.UTF_8)
                                    .ifBlank { download.request.id },
                                maxLines = 1
                            )
                        },
                        supportingContent = {
                            when (download.state) {
                                Download.STATE_COMPLETED -> Text("Available offline")
                                Download.STATE_FAILED -> Text("Download failed")
                                Download.STATE_DOWNLOADING -> {
                                    Column {
                                        Text("Downloading ${percent.roundToInt()}%")
                                        LinearProgressIndicator(progress = { (percent / 100f).coerceIn(0f,1f) })
                                    }
                                }
                                Download.STATE_QUEUED -> Text("Queued")
                                Download.STATE_STOPPED -> Text("Paused")
                                else -> Text("Preparing")
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = download.state == Download.STATE_COMPLETED,
                                    onClick = { onPlayDownload(download) }
                                ) { Icon(Icons.Rounded.PlayArrow, "Play offline") }
                                IconButton(
                                    onClick = { repository.remove(download.request.id) }
                                ) { Icon(Icons.Rounded.Delete, "Remove download") }
                            }
                        }
                    )
                }
            }
        }
    }
}
