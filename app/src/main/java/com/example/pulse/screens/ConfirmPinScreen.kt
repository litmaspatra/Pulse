package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun ConfirmPinScreen(
    onPinConfirmed: (String) -> Boolean
) {

    PinScreen(
        title = "Confirm PIN",
        onPinEntered = onPinConfirmed
    )

}