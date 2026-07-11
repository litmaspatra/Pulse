package com.example.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pulse.data.PinStorage
import com.example.pulse.navigation.AppNavigation
import com.example.pulse.ui.theme.AccentColor
import com.example.pulse.ui.theme.AppFontOption
import com.example.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pinStorage = remember { PinStorage(applicationContext) }
            val accentName by pinStorage.accentColorFlow.collectAsStateWithLifecycle(initialValue = null)
            val fontName by pinStorage.fontOptionFlow.collectAsStateWithLifecycle(initialValue = null)

            PulseTheme(
                accentColor = AccentColor.fromName(accentName).color,
                fontFamily = AppFontOption.fromName(fontName).fontFamily
            ) {
                AppNavigation()
            }
        }
    }
}