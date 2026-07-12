package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pulse.components.Keypad
import com.example.pulse.components.PinIndicator

@Composable
fun PinEntryScreen(
    title: String,
    subtitle: String? = null,
    onPinEntered: (String) -> Boolean
) {
    var enteredDigits by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var shouldShake by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.height(48.dp))
        PinIndicator(enteredDigits = enteredDigits, shouldShake = shouldShake)
        Spacer(modifier = Modifier.height(48.dp))
        Keypad(
            onNumberPressed = { digit ->
                if (enteredDigits < 4) {
                    pin += digit
                    enteredDigits++
                    if (enteredDigits == 4) {
                        val result = onPinEntered(pin)
                        if (!result) {
                            shouldShake = true
                            pin = ""
                            enteredDigits = 0
                        }
                    }
                }
            },
            onBackspacePressed = {
                if (enteredDigits > 0) {
                    pin = pin.dropLast(1)
                    enteredDigits--
                }
            }
        )
    }

    if (shouldShake) {
        // Reset shake after animation completes
        androidx.compose.runtime.LaunchedEffect(shouldShake) {
            kotlinx.coroutines.delay(400)
            shouldShake = false
        }
    }
}