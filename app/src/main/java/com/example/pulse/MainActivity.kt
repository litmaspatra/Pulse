package com.example.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pulse.data.PinStorage
import com.example.pulse.navigation.AppNavigation
import com.example.pulse.navigation.ChatScreen
import com.example.pulse.ui.theme.AccentColor
import com.example.pulse.ui.theme.AppFontOption
import com.example.pulse.ui.theme.PulseTheme

enum class AppState { LOCKED, UNLOCKED, IN_CHAT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pinStorage = remember { PinStorage(applicationContext) }
            val accentName by pinStorage.accentColorFlow.collectAsStateWithLifecycle(initialValue = null)
            val fontName by pinStorage.fontOptionFlow.collectAsStateWithLifecycle(initialValue = null)
            val primaryPin by pinStorage.primaryPinFlow.collectAsStateWithLifecycle(initialValue = null)

            var appState by remember { mutableStateOf(AppState.LOCKED) }

            PulseTheme(
                accentColor = AccentColor.fromName(accentName).color,
                fontFamily = AppFontOption.fromName(fontName).fontFamily
            ) {
                when (appState) {
                    AppState.LOCKED -> {
                        // Your PinEntryScreen goes here.
                        // On correct PIN: appState = AppState.UNLOCKED
                        // If no PIN is set yet (first launch), show PIN creation.
                        // This composable calls onUnlocked = { appState = AppState.UNLOCKED }
                        PinEntryScreen(
                            existingPin = primaryPin,
                            onUnlocked = { appState = AppState.UNLOCKED }
                        )
                    }

                    AppState.UNLOCKED -> {
                        AppNavigation(
                            onEnterChat = { appState = AppState.IN_CHAT },
                            onLockApp = { appState = AppState.LOCKED }
                        )
                    }

                    AppState.IN_CHAT -> {
                        ChatScreen(
                            onExitChat = {
                                appState = AppState.LOCKED
                                moveTaskToBack(true)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock when app goes to background.
        // Your PinEntryScreen should check lock timeout in PinStorage
        // and decide whether to actually show the PIN or let the user
        // straight back in (e.g., if timeout is 5 minutes and they were
        // away for 10 seconds).
        // For now, simplest correct behavior: always re-lock.
        // LockTimeout handling can be added inside PinEntryScreen itself
        // by comparing lastForegroundTime with the chosen timeout.
    }
}

// Temporary placeholder — replace with your actual PinEntryScreen
@androidx.compose.runtime.Composable
private fun PinEntryScreen(
    existingPin: String?,
    onUnlocked: () -> Unit
) {
    // Your existing PIN entry UI lives here.
    // When the user enters the correct PIN, call onUnlocked().
    // If existingPin is null, this is first launch — show PIN creation flow.
    // This is NOT a navigation destination, so back press here = minimize app.
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text("PIN Screen — replace with your composable")
    }
}

private fun androidx.compose.ui.Modifier.fillMaxSize(): androidx.compose.ui.Modifier =
    this.then(androidx.compose.foundation.layout.fillMaxSize())