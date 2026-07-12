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
import com.example.pulse.ui.theme.AccentColor
import com.example.pulse.ui.theme.AppFontOption
import com.example.pulse.viewmodel.SettingsViewModel
import com.example.pulse.viewmodel.SettingsViewModelFactory

@Composable
fun SettingsAppearanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val accentName by viewModel.accentColorName.collectAsStateWithLifecycle()
    val fontName by viewModel.fontOptionName.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
            Text("Accent Color", style = MaterialTheme.typography.titleSmall)
            AccentColor.entries.forEach { option ->
                val selected = option.name == accentName
                RadioButton(selected = selected, onClick = { viewModel.setAccentColor(option.name) },
                    colors = androidx.compose.material3.RadioButtonColors(
                        selectedColor = option.color,
                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
                { Text(option.label, style = MaterialTheme.typography.bodyLarge) }
            }

            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Font", style = MaterialTheme.typography.titleSmall)
            AppFontOption.entries.forEach { option ->
                val selected = option.name == fontName
                RadioButton(selected = selected, onClick = { viewModel.setFontOption(option.name) })
                { Text(option.label, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}