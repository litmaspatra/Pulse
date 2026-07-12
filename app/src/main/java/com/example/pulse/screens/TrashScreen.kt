package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.pulse.viewmodel.JournalViewModel
import com.example.pulse.viewmodel.JournalViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: JournalViewModel = viewModel(factory = JournalViewModelFactory(context))
    val trash by viewModel.trashEntries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isTrashLoading.collectAsStateWithLifecycle()
    var showEmptyConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadTrash() }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Empty Trash?") },
            text = { Text("This will permanently delete all trashed entries. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showEmptyConfirm = false; viewModel.emptyTrash() }) {
                    Text("Empty Trash")
                }
            },
            dismissButton = { TextButton(onClick = { showEmptyConfirm = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trash.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirm = true }) { Text("Empty") }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                trash.isEmpty() -> {
                    Text("Trash is empty",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(trash, key = { it.fileName }) { entry ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.dateLabel, style = MaterialTheme.typography.titleSmall)
                                        if (entry.preview.isNotBlank()) {
                                            Text(entry.preview, style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.restoreFromTrash(entry.fileName) }) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                                    }
                                    IconButton(onClick = { viewModel.permanentlyDelete(entry.fileName) }) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}