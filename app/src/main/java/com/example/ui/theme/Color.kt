package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Palette (Matching Original Haribansho App Design)
val LightBg = Color(0xFFF4F6F8)          // Clean off-white background
val LightSurface = Color(0xFFFFFFFF)     // Pure white card background
val LightSurfaceElevated = Color(0xFFFFFFFF) // White elevated surface
val LightBorder = Color(0xFFE2E8F0)      // Soft card border

val EmeraldPrimary = Color(0xFF00875A)   // Haribansho Deep Forest Green
val EmeraldDark = Color(0xFF006644)      // Deep Forest Green
val EmeraldLight = Color(0xFF00875A)     // Haribansho Primary Green
val EmeraldSurface = Color(0xFFE8F5E9)   // Light Green Pill Container
val EmeraldGlow = Color(0x2200875A)

val AmberAlert = Color(0xFFD97706)       // Amber warning/COD
val AmberSurface = Color(0xFFFEF3C7)     // Light Amber Pill Background
val RedDanger = Color(0xFFDC2626)        // Red danger
val RedSurface = Color(0xFFFEE2E2)       // Light Red Pill Background
val CyanInfo = Color(0xFF0284C7)         // Sky blue
val CyanSurface = Color(0xFFE0F2FE)      // Light Blue Pill Background

// Text Colors
val TextPrimary = Color(0xFF0F172A)      // Dark slate primary text
val TextSecondary = Color(0xFF64748B)    // Slate 500 secondary text
val TextMuted = Color(0xFF94A3B8)        // Slate 400 muted text

// Light mode mappings for all surfaces
val DarkBg = LightBg
val DarkSurface = LightSurface
val DarkSurfaceElevated = LightSurfaceElevated
val DarkBorder = LightBorder

// Backward compatibility mappings
val HaribanshoPrimary = EmeraldPrimary
val HaribanshoPrimaryVariant = EmeraldDark
val HaribanshoSecondary = EmeraldLight
val HaribanshoDarkGreen = EmeraldDark
val HaribanshoBackground = LightBg
val HaribanshoSurface = LightSurface
val HaribanshoTextPrimary = TextPrimary
val HaribanshoTextSecondary = TextSecondary
val HaribanshoTextMuted = TextMuted
val HaribanshoSuccess = EmeraldPrimary
val HaribanshoWarning = AmberAlert
val HaribanshoDanger = RedDanger
val HaribanshoInfo = CyanInfo
val HaribanshoCardBorder = LightBorder
val HaribanshoGreenSurface = EmeraldSurface

