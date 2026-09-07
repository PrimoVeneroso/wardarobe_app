package com.armadio.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Sage-and-ink palette; refined during the F1 design pass.

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3A6A4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBCF0C6),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF506356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D6),
    onSecondaryContainer = Color(0xFF0D1F12),
    tertiary = Color(0xFF815542),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FAF4),
    onBackground = Color(0xFF171B17),
    surface = Color(0xFFF7FAF4),
    onSurface = Color(0xFF171B17),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF404943),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA1D4AB),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF205131),
    onPrimaryContainer = Color(0xFFBCF0C6),
    secondary = Color(0xFFB7CCB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF394B3D),
    onSecondaryContainer = Color(0xFFD3E8D6),
    tertiary = Color(0xFFF3B9A5),
    onTertiary = Color(0xFF4A2616),
    background = Color(0xFF0F120F),
    onBackground = Color(0xFFDEE4DC),
    surface = Color(0xFF0F120F),
    onSurface = Color(0xFFDEE4DC),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
