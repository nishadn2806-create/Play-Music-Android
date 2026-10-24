package com.playmusic.app
import com.playmusic.app.online.OnlineMediaCache

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    // Phase 9: OnlineMediaCache is available for wiring the Media3 player data source.

    companion object {
        const val ACTION_SLEEP = "com.playmusic.app.SLEEP_TIMER"
        const val EXTRA_MINUTES = "minutes"
    }
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private val handler = Handler(Looper.getMainLooper())
    private var sleepEnd = 0L

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        session = MediaSession.Builder(this, player).setSessionActivity(pi).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SLEEP) {
            val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
            handler.removeCallbacksAndMessages(null)
            sleepEnd = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0L
            if (sleepEnd > 0) checkSleep()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun checkSleep() {
        handler.postDelayed({
            if (sleepEnd > 0 && System.currentTimeMillis() >= sleepEnd) {
                player.pause()
                sleepEnd = 0L
            } else if (sleepEnd > 0) checkSleep()
        }, 1000L)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        session.release()
        player.release()
        super.onDestroy()
    }
}
