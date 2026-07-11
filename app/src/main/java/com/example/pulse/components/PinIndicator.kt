package com.example.pulse.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp


@Composable
fun PinIndicator(
    enteredDigits: Int,
    shouldShake: Boolean
)
{val offsetX = remember {
    Animatable(0f)
}

    LaunchedEffect(shouldShake) {

        if (shouldShake) {

            repeat(4) {

                offsetX.animateTo(
                    12f,
                    animationSpec = tween(40)
                )

                offsetX.animateTo(
                    -12f,
                    animationSpec = tween(40)
                )

            }

            offsetX.animateTo(
                0f,
                animationSpec = tween(40)
            )

        }

    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset {
                IntOffset(offsetX.value.toInt(), 0)
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->

        if (index < enteredDigits) {
            Text("●")
        } else {
            Text("○")
        }

    }

    }

}