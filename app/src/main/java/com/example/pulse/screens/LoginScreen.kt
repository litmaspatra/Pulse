package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun LoginScreen(
    onPinEntered: (String) -> Boolean
) {
    PinEntryScreen(
        title = "Welcome Back",
        onPinEntered = onPinEntered
    )
}