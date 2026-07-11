// FILE: app/src/main/java/com/example/pulse/screens/JournalScreen.kt
package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.data.JournalEntry
import com.example.pulse.viewmodel.JournalStats
import com.example.pulse.viewmodel.JournalViewModel
import com.example.pulse.viewmodel.JournalViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onEntryClick: (String) -> Unit,
    onNewEntry: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: JournalViewModel = viewModel(factory = JournalViewModelFactory(context))

    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var entryPendingTrash by remember { mutableStateOf<JournalEntry?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadEntries() }

    val filtered = remember(entries, searchQuery) { viewModel.filteredEntries() }

    entryPendingTrash?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingTrash = null },
            title = { Text("Move to Trash?") },
            text = { Text("${entry.dateLabel} will be moved to Trash. You can restore it later.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.trashEntry(entry.fileName)
                    entryPendingTrash = null
                }) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingTrash = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Calendar") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            onClick = { showMenu = false; onOpenCalendar() }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = { showMenu = false; onOpenArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("Trash") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onOpenTrash() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewEntry(viewModel.newEntryFileName()) }) {
                Icon(Icons.Default.Add, contentDescription = "New entry")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (entries.isNotEmpty()) {
                StatsRow(stats = stats)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search entries...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    entries.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No entries yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + to write today's entry",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    filtered.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No matches", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.fileName }) { entry ->
                                JournalEntryRow(
                                    entry = entry,
                                    onClick = { onEntryClick(entry.fileName) },
                                    onArchiveClick = { viewModel.archiveEntry(entry.fileName) },
                                    onTrashClick = { entryPendingTrash = entry }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: JournalStats) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatChip("${stats.totalEntries}", "entries")
        StatChip("${stats.totalWords}", "words")
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (stats.currentStreak > 0) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            StatChip("${stats.currentStreak}", "day streak")
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            " $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun JournalEntryRow(
    entry: JournalEntry,
    onClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.dateLabel, style = MaterialTheme.typography.titleSmall)
                if (entry.preview.isNotBlank()) {
                    Text(
                        entry.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onArchiveClick) {
                Icon(Icons.Default.Archive, contentDescription = "Archive entry")
            }
            IconButton(onClick = onTrashClick) {
                Icon(Icons.Default.Delete, contentDescription = "Move to Trash")
            }
        }
    }
}