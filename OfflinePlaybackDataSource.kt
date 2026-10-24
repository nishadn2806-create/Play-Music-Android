package com.playmusic.app.online

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource

/**
 * Cache-aware factory for playback of downloaded media.
 * Media3 reads downloaded bytes from the Cache; the app should never read
 * files from the download directory directly.
 */
object OfflinePlaybackDataSource {
    fun factory(context: Context): DataSource.Factory =
        CacheDataSource.Factory()
            .setCache(OfflineDownloadManager.get(context).downloadCache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
