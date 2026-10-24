package com.playmusic.app.online

import android.content.Context
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager

class DownloadRepository(context: Context) {
    private val appContext = context.applicationContext
    private val manager: DownloadManager
        get() = OfflineDownloadManager.get(appContext)

    fun all(): List<Download> = OfflineDownloadManager.downloads(appContext)

    fun current(): List<Download> = manager.currentDownloads

    fun pauseAll() = OfflineDownloadManager.pauseAll(appContext)
    fun resumeAll() = OfflineDownloadManager.resumeAll(appContext)
    fun remove(id: String) = OfflineDownloadManager.remove(appContext, id)

    fun addListener(listener: DownloadManager.Listener) {
        manager.addListener(listener)
    }

    fun removeListener(listener: DownloadManager.Listener) {
        manager.removeListener(listener)
    }
}
