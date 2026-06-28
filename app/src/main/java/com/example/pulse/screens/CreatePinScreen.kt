package com.example.pulse.screens

import androidx.compose.runtime.Composable

@Composable
fun CreatePinScreen(
    onPinCreated: (String) -> Boolean
) {

    PinScreen(
        title = "Create PIN",
        onPinEntered = onPinCreated
    )

}