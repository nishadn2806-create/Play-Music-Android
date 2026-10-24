package com.playmusic.app.online

import androidx.media3.exoplayer.offline.Download

enum class HybridLibrarySection { LOCAL, DOWNLOADS, ONLINE }

data class HybridLibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: HybridLibrarySection,
    val download: Download? = null
)

object HybridLibrary {
    fun downloadedItems(repository: DownloadRepository): List<HybridLibraryItem> =
        repository.all()
            .filter { it.state == Download.STATE_COMPLETED }
            .map {
                HybridLibraryItem(
                    id = it.request.id,
                    title = String(it.request.data, Charsets.UTF_8).ifBlank { it.request.id },
                    subtitle = "Available offline",
                    section = HybridLibrarySection.DOWNLOADS,
                    download = it
                )
            }
}
