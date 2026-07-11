package com.example.pulse.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class AccentColor(val label: String, val color: Color) {
    LAVENDER("Lavender", Color(0xFF6650A4)),
    TEAL("Teal", Color(0xFF00696B)),
    ROSE("Rose", Color(0xFF8E4956)),
    FOREST("Forest", Color(0xFF3C6E44)),
    SLATE("Slate", Color(0xFF47586E));

    companion object {
        fun fromName(name: String?): AccentColor =
            entries.find { it.name == name } ?: LAVENDER
    }
}

enum class AppFontOption(val label: String, val fontFamily: FontFamily) {
    DEFAULT("Default", FontFamily.Default),
    SERIF("Serif", FontFamily.Serif),
    MONOSPACE("Monospace", FontFamily.Monospace);

    companion object {
        fun fromName(name: String?): AppFontOption =
            entries.find { it.name == name } ?: DEFAULT
    }
}