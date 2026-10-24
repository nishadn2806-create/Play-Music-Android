package com.playmusic.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class LibraryTab(val label: String, val icon: ImageVector) {
    Albums("Albums", Icons.Rounded.Album),
    Artists("Artists", Icons.Rounded.Person),
    Folders("Folders", Icons.Rounded.Folder)
}

@Composable
fun LibraryBrowseScreen(
    songs: List<Song>,
    onOpenGroup: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var selected by remember { mutableStateOf(LibraryTab.Albums) }
    val groups = remember(songs) { MediaStoreLibraryRepository.groupsFrom(songs) }
    val visible = remember(groups, selected) {
        val prefix = when (selected) {
            LibraryTab.Albums -> "album:"
            LibraryTab.Artists -> "artist:"
            LibraryTab.Folders -> "folder:"
        }
        groups.filter { it.key.startsWith(prefix) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${songs.size} songs", color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }

        PrimaryTabRow(selectedTabIndex = selected.ordinal) {
            LibraryTab.values().forEachIndexed { index, tab ->
                Tab(
                    selected = selected.ordinal == index,
                    onClick = { selected = tab },
                    text = { Text(tab.label) },
                    icon = { Icon(tab.icon, null) }
                )
            }
        }

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(selected.icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text("No ${selected.label.lowercase()} found")
                    Text("Refresh after adding music to your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(visible, key = { it.key }) { group ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenGroup(group.key) },
                        headlineContent = { Text(group.title, maxLines = 1) },
                        supportingContent = { Text(group.subtitle, maxLines = 1) },
                        leadingContent = { Icon(selected.icon, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Rounded.MusicNote, null) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
