package com.playmusic.app.online

import android.app.Notification
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService

@UnstableApi
class OfflineDownloadService : DownloadService(
    1001,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    "offline_downloads",
    0,
    0
) {
    override fun getDownloadManager(): DownloadManager =
        OfflineDownloadManager.get(this)

    override fun getScheduler() = null

    override fun getForegroundNotification(
        downloads: List<androidx.media3.exoplayer.offline.Download>,
        notMetRequirements: Int
    ): Notification {
        return DownloadNotificationHelper(
            this,
            "offline_downloads"
        ).buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            "Downloading music",
            downloads,
            notMetRequirements
        )
    }

    companion object {
        fun isAvailable(context: Context): Boolean = true
    }
}
