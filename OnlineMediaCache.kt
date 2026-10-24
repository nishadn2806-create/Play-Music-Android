package com.playmusic.app.online

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Small, bounded on-the-fly cache for online playback.
 * This is streaming cache, not permanent offline download storage.
 */
object OnlineMediaCache {
    private const val MAX_CACHE_BYTES = 150L * 1024L * 1024L
    @Volatile private var cache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, "online_media"),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { cache = it }
        }

    fun dataSourceFactory(
        context: Context,
        upstream: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
    ): DataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
