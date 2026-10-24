package com.playmusic.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playmusic.app.online.DownloadRepository

@Composable
fun HybridLibraryScreen(
    repository: DownloadRepository,
    onOpenDownloads: () -> Unit,
    onOpenLocalMusic: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)
        Text(
            "One place for your local music and downloaded online tracks.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedCard(
            Modifier.fillMaxWidth(),
            onClick = onOpenLocalMusic
        ) {
            ListItem(
                headlineContent = { Text("Local Music") },
                supportingContent = { Text("Songs already stored on your device") },
                leadingContent = { Icon(Icons.Rounded.LibraryMusic, null) }
            )
        }

        OutlinedCard(
            Modifier.fillMaxWidth(),
            onClick = onOpenDownloads
        ) {
            ListItem(
                headlineContent = { Text("My Downloads") },
                supportingContent = { Text("Online songs saved for offline playback") },
                leadingContent = { Icon(Icons.Rounded.Download, null) }
            )
        }
    }
}
