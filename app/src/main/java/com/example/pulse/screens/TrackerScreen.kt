package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TrackerScreen() {

    var waterCount by remember {
        mutableStateOf(0)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "🌱 Plant Care",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Watered Today: $waterCount",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = {
                waterCount++
            }
        ) {

            Text("Water Plant")

        }

    }

}