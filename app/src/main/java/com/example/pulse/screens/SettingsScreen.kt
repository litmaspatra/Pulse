// FILE: app/src/main/java/com/example/pulse/screens/SettingsScreen.kt
package com.example.pulse.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.data.LockTimeout
import com.example.pulse.ui.theme.AccentColor
import com.example.pulse.ui.theme.AppFontOption
import com.example.pulse.viewmodel.ImportOutcome
import com.example.pulse.viewmodel.SettingsViewModel
import com.example.pulse.viewmodel.SettingsViewModelFactory

private const val TAPS_TO_TRIGGER = 7
private const val TAP_RESET_WINDOW_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSetupSecondPin: () -> Unit,
    onChangePrimaryPin: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    val accentName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val fontName by viewModel.fontOptionName.collectAsStateWithLifecycle()
    val lockTimeoutName by viewModel.lockTimeoutName.collectAsStateWithLifecycle()
    val exportUri by viewModel.exportResult.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    LaunchedEffect(exportUri) {
        val uri = exportUri ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Save or share journal export"))
        viewModel.clearExportResult()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importJournal(uri) }

    importResult?.let { outcome ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = { Text(if (outcome is ImportOutcome.Success) "Import complete" else "Import failed") },
            text = {
                Text(
                    when (outcome) {
                        is ImportOutcome.Success -> {
                            val base = "Imported ${outcome.imported} ${if (outcome.imported == 1) "entry" else "entries"}."
                            if (outcome.skipped > 0) "$base Skipped ${outcome.skipped} already present." else base
                        }
                        is ImportOutcome.Failed -> "Couldn't read that file. Make sure it's a Pulse journal export."
                    }
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.clearImportResult() }) { Text("OK") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Text("Accent color", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AccentColor.entries.forEach { option ->
                        val selected = option.name == accentName || (accentName == null && option == AccentColor.LAVENDER)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .clickable { viewModel.setAccentColor(option.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            Column {
                Text("Font", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFontOption.entries.forEach { option ->
                        val selected = option.name == fontName || (fontName == null && option == AppFontOption.DEFAULT)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFontOption(option.name) },
                            label = { Text(option.label, fontFamily = option.fontFamily) }
                        )
                    }
                }
            }

            Column {
                Text("Privacy", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = onChangePrimaryPin,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Change Journal PIN") }

                Text(
                    "Lock timeout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockTimeout.entries.forEach { option ->
                        val selected = option.name == lockTimeoutName || (lockTimeoutName == null && option == LockTimeout.IMMEDIATE)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setLockTimeout(option.name) },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Column {
                Text("Data", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportJournal() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export Journal") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import Journal") }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().fillMaxSize()) {
                Text(
                    text = "v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val now = System.currentTimeMillis()
                            tapCount = if (now - lastTapTime > TAP_RESET_WINDOW_MS) 1 else tapCount + 1
                            lastTapTime = now
                            if (tapCount >= TAPS_TO_TRIGGER) {
                                tapCount = 0
                                onSetupSecondPin()
                            }
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}