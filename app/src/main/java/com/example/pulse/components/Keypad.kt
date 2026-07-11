package com.example.pulse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Keypad(
    onNumberPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit
) {
    val rows = listOf(

        listOf(
            Key(KeyType.NUMBER, label = "1"),
            Key(KeyType.NUMBER, label = "2"),
            Key(KeyType.NUMBER, label = "3")
        ),

        listOf(
            Key(KeyType.NUMBER, label = "4"),
            Key(KeyType.NUMBER, label = "5"),
            Key(KeyType.NUMBER, label = "6")
        ),

        listOf(
            Key(KeyType.NUMBER, label = "7"),
            Key(KeyType.NUMBER, label = "8"),
            Key(KeyType.NUMBER, label = "9")
        ),

        listOf(
            Key(
                type = KeyType.BACKSPACE,
                icon = Icons.AutoMirrored.Filled.Backspace
            ),

            Key(
                type = KeyType.NUMBER,
                label = "0"
            ),

            Key(
                type = KeyType.EMPTY
            )
        )
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

                row.forEach { key ->

                    when (key.type) {

                        KeyType.NUMBER -> {

                            KeypadButton(
                                key = key,
                                onClick = {
                                    onNumberPressed(key.label!!)
                                }
                            )

                        }

                        KeyType.BACKSPACE -> {

                            KeypadButton(
                                key = key,
                                onClick = {
                                    onBackspacePressed()
                                }
                            )

                        }

                        KeyType.EMPTY -> {

                            Spacer(
                                modifier = Modifier.size(72.dp)
                            )

                        }
                    }

                }

            }

        }

    }
}




