package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pulse.components.Keypad
import com.example.pulse.components.PinIndicator
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun PinEntryScreen(
    title: String,
    onPinEntered: (String) -> Boolean
) {
    val haptic = LocalHapticFeedback.current

    var pin by remember {
        mutableStateOf("")
    }
    var shouldShake by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(shouldShake) {

        if (shouldShake) {

            delay(350)

            pin = ""

            shouldShake = false

        }

    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "🌱 Pulse",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )

        PinIndicator(
            enteredDigits = pin.length,
            shouldShake = shouldShake
        )
        Keypad (

            onNumberPressed = { digit ->

                if (pin.length < 4) {

                    pin += digit

                    if (pin.length == 4) {

                        val success = onPinEntered(pin)

                        if (!success) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            shouldShake = true

                        }

                    }

                }

            },

            onBackspacePressed = {

                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }

            }

        )
    }
}