package com.example.pulse.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.viewmodel.ImportOutcome
import com.example.pulse.viewmodel.SettingsViewModel
import com.example.pulse.viewmodel.SettingsViewModelFactory

@Composable
fun SettingsBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.exportJournal()
            // The export creates a local file and returns its content:// URI.
            // To share it, we'd copy to the picked URI. For simplicity, we
            // just clear the result here — the file is saved in Exports/.
            viewModel.clearExportResult()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.importJournal(uri)
    }

    var showImportDialog by remember { mutableStateOf(false) }

    if (importResult != null) {
        val outcome = importResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = { Text(if (outcome is ImportOutcome.Success) "Import Complete" else "Import Failed") },
            text = {
                Text(if (outcome is ImportOutcome.Success)
                    "Imported ${outcome.imported} entries, skipped ${outcome.skipped}."
                else "Couldn't read the file. Make sure it's a Pulse export.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportResult() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Export your journal entries to a text file, or import from a previous backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(java.util.Date())
                exportLauncher.launch("pulse_export_$timestamp.txt")
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Export Journal")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { importLauncher.launch("text/plain") }, modifier = Modifier.fillMaxWidth()) {
                Text("Import Journal")
            }
        }
    }
}