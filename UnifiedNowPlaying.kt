package com.playmusic.app

import androidx.media3.common.Player

data class NowPlayingState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueIndex: Int = -1,
    val queueSize: Int = 0
)

fun Player.toNowPlayingState(): NowPlayingState {
    val item = currentMediaItem
    return NowPlayingState(
        title = item?.mediaMetadata?.title?.toString().orEmpty(),
        artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
        album = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
        isPlaying = isPlaying,
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = duration.takeIf { it > 0 } ?: 0L,
        queueIndex = currentMediaItemIndex,
        queueSize = mediaItemCount
    )
}
