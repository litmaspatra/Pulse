package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pulse.viewmodel.PairingState

@Composable
fun PairingScreen(
    pairingState: PairingState,
    roomCode: String?,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onErrorDismiss: () -> Unit
) {
    var joinCode by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (pairingState) {
            is PairingState.Idle -> {
                Text("Private Chat", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Create a room or join with a code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onCreateRoom, modifier = Modifier.fillMaxWidth()) {
                    Text("Create Room")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { if (it.length <= 6) joinCode = it.filter { c -> c.isDigit() } },
                    label = { Text("Room code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { if (joinCode.length == 6) onJoinRoom(joinCode) },
                    enabled = joinCode.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join Room") }
            }

            is PairingState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Creating room...")
            }

            is PairingState.WaitingForPartner -> {
                Text("Your Code", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(roomCode ?: "", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(roomCode ?: ""))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
                    }
                    Text("Tap to copy", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Waiting for partner to join...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            is PairingState.Paired -> {
                Text("Connected!", style = MaterialTheme.typography.titleMedium)
            }

            is PairingState.Error -> {
                Text("Error", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Text(pairingState.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onErrorDismiss) { Text("Try Again") }
            }
        }
    }
}