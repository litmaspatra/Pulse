package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onVerifySecondPin: (String) -> Boolean,
    onDisableChat: () -> Unit,
    onChangePin: () -> Unit
) {
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect?") },
            text = { Text("You'll leave this chat. Chat history stays if you reconnect later.") },
            confirmButton = {
                TextButton(onClick = { showDisconnectConfirm = false; onDisconnect() }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false; codeInput = ""; codeError = false },
            title = { Text("Disable private chat") },
            text = {
                Column {
                    Text("Enter your chat PIN to confirm. You'll need to set it up again from Settings to re-enable.")
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 4) { codeInput = it; codeError = false } },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = codeError,
                        singleLine = true
                    )
                    if (codeError) {
                        Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onVerifySecondPin(codeInput)) {
                        showDisableDialog = false
                        onDisableChat()
                    } else {
                        codeError = true
                    }
                }) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false; codeInput = ""; codeError = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onChangePin, modifier = Modifier.fillMaxWidth()) {
                Text("Change chat PIN")
            }

            OutlinedButton(onClick = { showDisconnectConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Disconnect")
            }

            Button(
                onClick = { showDisableDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Disable private chat") }
        }
    }
}