package com.vista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val VistaDarkColorScheme = darkColorScheme(
    primary = VistaColors.Gold,
    onPrimary = VistaColors.Background,

    secondary = VistaColors.VoiceBlue,
    onSecondary = VistaColors.Background,

    background = VistaColors.Background,
    onBackground = VistaColors.TextPrimary,

    surface = VistaColors.Surface,
    onSurface = VistaColors.TextPrimary,

    surfaceVariant = VistaColors.MidnightBlue,
    onSurfaceVariant = VistaColors.TextSecondary,

    error = VistaColors.Error,
    onError = Color.White
)

@Composable
fun VistaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VistaDarkColorScheme,
        typography = VistaTypography
    ) {
        CompositionLocalProvider(LocalContentColor provides VistaColors.TextPrimary) {
            content()
        }
    }
}
