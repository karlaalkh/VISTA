package com.voya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoyaDarkColorScheme = darkColorScheme(
    primary = VoyaColors.Gold,
    onPrimary = VoyaColors.Background,

    secondary = VoyaColors.VoiceBlue,
    onSecondary = VoyaColors.Background,

    background = VoyaColors.Background,
    onBackground = VoyaColors.TextPrimary,

    surface = VoyaColors.Surface,
    onSurface = VoyaColors.TextPrimary,

    surfaceVariant = VoyaColors.MidnightBlue,
    onSurfaceVariant = VoyaColors.TextSecondary,

    error = VoyaColors.Error,
    onError = Color.White
)

@Composable
fun VoyaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VoyaDarkColorScheme,
        typography = VoyaTypography,
        content = content
    )
}