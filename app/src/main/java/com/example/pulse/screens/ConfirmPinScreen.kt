package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun ConfirmPinScreen(
    onPinEntered: (String) -> Boolean
) {
    PinEntryScreen(
        title = "Confirm PIN",
        onPinEntered = onPinEntered
    )
}