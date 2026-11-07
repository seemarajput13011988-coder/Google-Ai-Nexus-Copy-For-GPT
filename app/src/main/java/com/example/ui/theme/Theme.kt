package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlueTint,
    onPrimaryContainer = ElectricBlue,
    secondary = VividCyan,
    onSecondary = Color.White,
    secondaryContainer = LightCyanTint,
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = TelecomOrange,
    onTertiary = Color.White,
    tertiaryContainer = LightAmberTint,
    onTertiaryContainer = Color(0xFF9A3412),
    background = BrightBackground,
    onBackground = TextCharcoal,
    surface = PureWhiteSurface,
    onSurface = TextCharcoal,
    surfaceVariant = BrightCardElevated,
    onSurfaceVariant = TextMuted,
    outline = BrightCardBorder,
    error = CoralRose
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = VividCyan,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = GoldenAmber,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = CoralRose
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Bright theme default!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
