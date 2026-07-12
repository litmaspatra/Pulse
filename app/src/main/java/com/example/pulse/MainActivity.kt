package com.example.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pulse.data.PinStorage
import com.example.pulse.navigation.AuthNavigation
import com.example.pulse.navigation.MainNavigation
import com.example.pulse.screens.ChatScreen
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

            var appState by remember { mutableStateOf(AppState.LOCKED) }

            PulseTheme(
                accentColor = AccentColor.fromName(accentName).color,
                fontFamily = AppFontOption.fromName(fontName).fontFamily
            ) {
                when (appState) {
                    AppState.LOCKED -> {
                        BackHandler { moveTaskToBack(true) }
                        AuthNavigation(
                            onUnlocked = { appState = AppState.UNLOCKED },
                            onOpenChat = { appState = AppState.IN_CHAT }
                        )
                    }
                    AppState.UNLOCKED -> {
                        BackHandler { moveTaskToBack(true) }
                        MainNavigation(
                            onEnterChat = { appState = AppState.IN_CHAT }
                        )
                    }
                    AppState.IN_CHAT -> {
                        // No BackHandler here — ChatScreen handles its own back
                        // by calling onExitChat which locks + minimizes
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
}