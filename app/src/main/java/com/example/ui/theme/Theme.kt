package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElegantBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,
    secondary = ElegantEmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSecondaryContainer,
    tertiary = ElegantCrimsonTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonTertiaryContainer,
    background = DarkBackground,
    onBackground = SlateBrightText,
    surface = DarkSurface,
    onSurface = SlateLightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SlateLightText,
    outline = DarkOutline,
    outlineVariant = DarkOutlineMuted
)

private val LightColorScheme = lightColorScheme(
    primary = ElegantBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,
    secondary = ElegantEmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSecondaryContainer,
    tertiary = ElegantCrimsonTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonTertiaryContainer,
    background = DarkBackground,  // Keep it elegant dark even in normal mode!
    onBackground = SlateBrightText,
    surface = DarkSurface,
    onSurface = SlateLightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SlateLightText,
    outline = DarkOutline,
    outlineVariant = DarkOutlineMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the elegant dark feeling
    dynamicColor: Boolean = false, // Set dynamic color false for explicit Elegant Dark customization
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

