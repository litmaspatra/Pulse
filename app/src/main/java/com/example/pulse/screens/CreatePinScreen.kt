package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun CreatePinScreen(
    onPinEntered: (String) -> Boolean
) {
    PinEntryScreen(
        title = "Create PIN",
        onPinEntered = onPinEntered
    )
}