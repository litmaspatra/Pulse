package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun LoginScreen(
    onPinEntered: (String) -> Boolean,
    onForgotPin: () -> Unit
) {
    PinEntryScreen(
        title = "Enter PIN",
        onPinEntered = onPinEntered
    )
    // Note: ForgotPin button can be added below the keypad if desired.
    // The onForgotPin callback is wired in AuthNavigation.
}