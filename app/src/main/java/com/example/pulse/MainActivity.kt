package com.example.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pulse.navigation.AppNavigation
import com.example.pulse.screens.PinScreen
import com.example.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to Pulse",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun PinScreenPreview() {
    PulseTheme {
        PinScreen(
            title = "Welcome Back",
            onPinEntered = { false }
        )
    }
}