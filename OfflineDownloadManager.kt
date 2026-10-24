package com.playmusic.app.online

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import java.io.File
import java.util.concurrent.Executors

/**
 * Persistent offline-download manager for online tracks.
 * This cache intentionally has NoOpCacheEvictor so completed downloads are retained.
 */
object OfflineDownloadManager {
    private const val MAX_PARALLEL_DOWNLOADS = 2
    private var manager: DownloadManager? = null
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context): DownloadManager {
        manager?.let { return it }
        val app = context.applicationContext
        val db = StandaloneDatabaseProvider(app)
        val downloadCache = SimpleCache(
            File(app.filesDir, "offline_downloads"),
            NoOpCacheEvictor(),
            db
        )
        cache = downloadCache

        return DownloadManager(
            app,
            db,
            downloadCache,
            DefaultHttpDataSource.Factory(),
            Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS)
        ).also {
            it.maxParallelDownloads = MAX_PARALLEL_DOWNLOADS
            manager = it
        }
    }

    fun add(context: Context, request: DownloadRequest) {
        OfflineDownloadService.sendAddDownload(
            context.applicationContext,
            OfflineDownloadService::class.java,
            request,
            false
        )
    }

    fun remove(context: Context, contentId: String) {
        OfflineDownloadService.sendRemoveDownload(
            context.applicationContext,
            OfflineDownloadService::class.java,
            contentId,
            false
        )
    }

    fun pauseAll(context: Context) {
        OfflineDownloadService.sendPauseDownloads(
            context.applicationContext,
            OfflineDownloadService::class.java,
            false
        )
    }

    fun resumeAll(context: Context) {
        OfflineDownloadService.sendResumeDownloads(
            context.applicationContext,
            OfflineDownloadService::class.java,
            false
        )
    }

    fun downloads(context: Context): List<Download> {
        val cursor = get(context).downloadIndex.getDownloads()
        val result = mutableListOf<Download>()
        while (cursor.moveToNext()) result += cursor.download
        cursor.close()
        return result
    }
}
