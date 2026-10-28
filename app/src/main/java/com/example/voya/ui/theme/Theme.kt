package com.example.voya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF245E57),
    secondary = Color(0xFF526460),
    tertiary = Color(0xFF47617A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91D5C9),
    secondary = Color(0xFFB3CCC5),
    tertiary = Color(0xFFAFCBE8),
)

@Composable
fun VoyaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
