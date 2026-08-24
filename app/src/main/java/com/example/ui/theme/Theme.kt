package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = HaribanshoPrimary,
    onPrimary = Color.White,
    primaryContainer = HaribanshoGreenSurface,
    onPrimaryContainer = HaribanshoDarkGreen,
    secondary = HaribanshoSecondary,
    onSecondary = Color.White,
    tertiary = HaribanshoPrimaryVariant,
    background = HaribanshoBackground,
    onBackground = HaribanshoTextPrimary,
    surface = HaribanshoSurface,
    onSurface = HaribanshoTextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = HaribanshoTextSecondary,
    error = HaribanshoDanger,
    onError = Color.White,
    outline = HaribanshoCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = HaribanshoSecondary,
    onPrimary = Color.Black,
    primaryContainer = HaribanshoDarkGreen,
    onPrimaryContainer = Color.White,
    secondary = HaribanshoPrimaryVariant,
    onSecondary = Color.White,
    background = Color(0xFF121815),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1A221E),
    onSurface = Color(0xFFF3F4F6),
    error = HaribanshoDanger,
    onError = Color.White,
    outline = Color(0xFF2C3831)
)

@Composable
fun HaribanshoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
