package com.example.pulse.screens

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LoginScreen(
    onPinEntered: (String) -> Boolean,
    onForgotPin: () -> Unit = {}
) {
    PinEntryScreen(
        title = "Welcome Back",
        onPinEntered = onPinEntered,
        footer = {
            TextButton(onClick = onForgotPin) { Text("Forgot PIN?") }
        }
    )
}