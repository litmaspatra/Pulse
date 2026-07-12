package com.example.pulse.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.data.LockTimeout
import com.example.pulse.viewmodel.SettingsViewModel
import com.example.pulse.viewmodel.SettingsViewModelFactory

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val timeoutName by viewModel.lockTimeoutName.collectAsStateWithLifecycle()

    // These need to come from the navigation caller in a real setup.
    // For now, these are placeholders — the actual PIN change flows are
    // handled by MainNavigation's CHANGE_PIN routes.
    val onChangePrimaryPin: () -> Unit = {}
    val onSetupSecondPin: () -> Unit = {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Change Primary PIN", style = MaterialTheme.typography.titleSmall)
            Text("Change the PIN you use to unlock the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            OutlinedButton(onClick = onChangePrimaryPin, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Change PIN")
            }

            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Lock Timeout", style = MaterialTheme.typography.titleSmall)
            Text("How quickly the app locks after going to background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            LockTimeout.entries.forEach { option ->
                val selected = option.name == timeoutName
                RadioButton(selected = selected, onClick = { viewModel.setLockTimeout(option.name) })
                { Text(option.label, style = MaterialTheme.typography.bodyLarge) }
            }

            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Chat PIN", style = MaterialTheme.typography.titleSmall)
            Text("Separate PIN to access private chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            OutlinedButton(onClick = onSetupSecondPin, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Set Up Chat PIN")
            }
        }
    }
}