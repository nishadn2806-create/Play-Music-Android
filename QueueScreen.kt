package com.playmusic.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

@Composable
fun QueueScreen(player: Player, onClose: () -> Unit = {}) {
    var items by remember { mutableStateOf(UnifiedQueueManager.snapshot(player)) }

    fun refresh() { items = UnifiedQueueManager.snapshot(player) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Queue", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = {
                UnifiedQueueManager.clear(player)
                refresh()
            }) { Text("Clear") }
        }

        if (items.isEmpty()) {
            Text("Your queue is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(items, key = { _, item -> item.mediaId }) { index, item ->
                    ListItem(
                        headlineContent = {
                            Text(item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Unknown title" })
                        },
                        supportingContent = {
                            Text(item.mediaMetadata.artist?.toString().orEmpty())
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.DragHandle, "Queue item")
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                UnifiedQueueManager.remove(player, index)
                                refresh()
                            }) {
                                Icon(Icons.Rounded.Delete, "Remove")
                            }
                        }
                    )
                }
            }
        }
    }
}
