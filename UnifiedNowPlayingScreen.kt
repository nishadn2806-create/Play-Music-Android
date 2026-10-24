package com.playmusic.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.delay

@Composable
fun UnifiedNowPlayingScreen(player: Player) {
    var state by remember { mutableStateOf(player.toNowPlayingState()) }

    LaunchedEffect(player) {
        while (true) {
            state = player.toNowPlayingState()
            delay(500)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Now Playing", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            state.title.ifBlank { "Nothing playing" },
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            listOf(state.artist, state.album).filter { it.isNotBlank() }.joinToString(" • "),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        val progress = if (state.durationMs > 0) {
            (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                Icon(Icons.Rounded.SkipPrevious, "Previous")
            }
            FilledIconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                Icon(
                    if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    "Play/Pause"
                )
            }
            IconButton(onClick = { player.seekToNextMediaItem() }) {
                Icon(Icons.Rounded.SkipNext, "Next")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("${state.queueIndex + 1} / ${state.queueSize}")
    }
}
