package com.example.pulse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace

@Composable
fun Keypad(
    onNumberPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    val buttonSpacing = 16.dp
    Column(
        verticalArrangement = Arrangement.spacedBy(buttonSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->

            Row(
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {

                row.forEach { number ->

                    KeypadButton(
                        label = number,
                        onClick = {
                            onNumberPressed(number)
                        }
                    )

                }

            }

        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {

            KeypadButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                onClick = {
                    onBackspacePressed()
                }
            )

            KeypadButton(
                label = "0",
                onClick = {
                    onNumberPressed("0")
                }
            )

            Spacer(
                modifier = Modifier.size(72.dp)
            )

        }

    }
}




