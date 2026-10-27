package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TaxiAmberBright,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = CyanAccent,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = EmeraldGreen,
    onTertiary = Color(0xFF000000),
    background = TaxiDarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = TaxiSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = TaxiSurfaceVariantDark,
    onSurfaceVariant = LightSlate,
    outline = TaxiBorderDark,
    error = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary = TaxiAmberLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = CyanAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = EmeraldGreen,
    onTertiary = Color(0xFFFFFFFF),
    background = TaxiBgLight,
    onBackground = Color(0xFF0F172A),
    surface = TaxiSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = TaxiSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = TaxiBorderLight,
    error = CoralRed
)

@Composable
fun TaxiGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    TaxiGoTheme(darkTheme = darkTheme, content = content)
}
