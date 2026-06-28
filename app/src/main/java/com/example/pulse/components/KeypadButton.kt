package com.example.pulse.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun KeypadButton(
    key: Key,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier.size(72.dp)
    ) {
        when {
            key.icon != null -> {
                Icon(
                    imageVector = key.icon,
                    contentDescription = null
                )
            }

            key.label != null -> {
                Text(key.label)
            }
        }
    }

}