package com.example.pulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha
)

private fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha
)

@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = AccentColor.LAVENDER.color,
    fontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val onAccent = if (accentColor.luminance() > 0.5f) Color.Black else Color.White

    // Only primary/secondary/tertiary were being overridden before — FAB and
    // chips use *Container variants, which stayed the old default purple.
    // That's why accent changes looked like they weren't applying.
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor.lighten(0.15f),
            onPrimary = onAccent,
            primaryContainer = accentColor.darken(0.35f),
            onPrimaryContainer = accentColor.lighten(0.7f),
            secondary = accentColor.lighten(0.35f),
            secondaryContainer = accentColor.darken(0.45f),
            tertiary = accentColor.lighten(0.5f)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = onAccent,
            primaryContainer = accentColor.lighten(0.75f),
            onPrimaryContainer = accentColor.darken(0.3f),
            secondary = accentColor.darken(0.15f),
            secondaryContainer = accentColor.lighten(0.85f),
            tertiary = accentColor.lighten(0.15f)
        )
    }

    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontSize = 12.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontSize = 24.sp)
    )

    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}