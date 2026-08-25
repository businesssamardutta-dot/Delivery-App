package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldSurface,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldLight,
    onSecondary = Color.White,
    tertiary = AmberAlert,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = RedDanger,
    onError = Color.White,
    outline = LightBorder
)

@Composable
fun HaribanshoTheme(
    darkTheme: Boolean = false, // Light mode original theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

