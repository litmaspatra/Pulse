package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun ConfirmPinScreen(
    onPinConfirmed: (String) -> Boolean
) {

    LoginScreen(
        title = "Confirm PIN",
        onPinEntered = onPinConfirmed
    )

}