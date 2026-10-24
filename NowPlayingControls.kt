package com.playmusic.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

@Composable
fun NowPlayingControlBar(
    player: Player,
    modifier: Modifier = Modifier
) {
    var shuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeat by remember { mutableStateOf(player.repeatMode) }
    var position by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(250)
        }
    }

    Column(modifier) {
        Slider(
            value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
            onValueChange = { value ->
                position = value.toLong()
            },
            onValueChangeFinished = {
                if (duration > 0) player.seekTo(position.coerceIn(0L, duration))
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat()
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                shuffle = !shuffle
                player.shuffleModeEnabled = shuffle
            }) {
                Icon(Icons.Rounded.Shuffle, if (shuffle) "Shuffle on" else "Shuffle off")
            }

            IconButton(onClick = { player.seekToPreviousMediaItem() }) {
                Icon(Icons.Rounded.SkipPrevious, "Previous")
            }

            FilledIconButton(onClick = {
                if (player.isPlaying) player.pause() else player.play()
            }) {
                Text(if (player.isPlaying) "Ⅱ" else "▶")
            }

            IconButton(onClick = { player.seekToNextMediaItem() }) {
                Icon(Icons.Rounded.SkipNext, "Next")
            }

            IconButton(onClick = {
                repeat = when (repeat) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                player.repeatMode = repeat
            }) {
                Icon(
                    if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                    else Icons.Rounded.Repeat,
                    "Repeat"
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position))
            Text(formatTime(duration))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
