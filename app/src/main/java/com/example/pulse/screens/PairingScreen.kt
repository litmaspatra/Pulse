package com.example.pulse.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulse.viewmodel.PairingState

@Composable
fun PairingScreen(
    pairingState: PairingState,
    roomCode: String?,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onErrorDismiss: () -> Unit
) {
    var enteredCode by remember { mutableStateOf("") }
    var showJoinField by remember { mutableStateOf(false) }

    // Auto dismiss error after 3 seconds
    LaunchedEffect(pairingState) {
        if (pairingState is PairingState.Error) {
            kotlinx.coroutines.delay(3000)
            onErrorDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "🌱",
            fontSize = 56.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pulse",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "your quiet connection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (pairingState) {

            is PairingState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            is PairingState.WaitingForPartner -> {
                Text(
                    text = "Your room code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Big code display
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = roomCode ?: "------",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Share this code with your partner.\nWaiting for them to join...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            is PairingState.Error -> {
                Text(
                    text = pairingState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Fall through to show buttons again
                PairingButtons(
                    showJoinField = showJoinField,
                    enteredCode = enteredCode,
                    onEnteredCodeChange = { if (it.length <= 6) enteredCode = it },
                    onCreateRoom = onCreateRoom,
                    onShowJoinField = { showJoinField = true },
                    onJoinRoom = { onJoinRoom(enteredCode) }
                )
            }

            else -> {
                PairingButtons(
                    showJoinField = showJoinField,
                    enteredCode = enteredCode,
                    onEnteredCodeChange = { if (it.length <= 6) enteredCode = it },
                    onCreateRoom = onCreateRoom,
                    onShowJoinField = { showJoinField = true },
                    onJoinRoom = { onJoinRoom(enteredCode) }
                )
            }
        }
    }
}

@Composable
private fun PairingButtons(
    showJoinField: Boolean,
    enteredCode: String,
    onEnteredCodeChange: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onShowJoinField: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Button(
        onClick = onCreateRoom,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Create a room")
    }

    Spacer(modifier = Modifier.height(12.dp))

    AnimatedVisibility(visible = !showJoinField) {
        OutlinedButton(
            onClick = onShowJoinField,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join with a code")
        }
    }

    AnimatedVisibility(visible = showJoinField) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = enteredCode,
                onValueChange = onEnteredCodeChange,
                label = { Text("Enter 6-digit code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoinRoom,
                enabled = enteredCode.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join room")
            }
        }
    }
}