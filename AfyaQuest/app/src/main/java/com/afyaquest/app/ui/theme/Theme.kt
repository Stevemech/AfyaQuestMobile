package com.afyaquest.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AfyaPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A3535),
    onPrimaryContainer = AfyaPrimaryContainer,
    secondary = AfyaSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5C3A1A),
    onSecondaryContainer = AfyaSecondaryContainer,
    tertiary = AfyaTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1A3540),
    onTertiaryContainer = AfyaTertiaryContainer,
    background = BackgroundDark,
    onBackground = Color(0xFFE0E3E2),
    surface = SurfaceDark,
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF243333),
    onSurfaceVariant = Color(0xFFBEC9C7),
    error = AfyaError,
    onError = Color.White,
    outline = Color(0xFF899391)
)

private val LightColorScheme = lightColorScheme(
    primary = AfyaPrimary,
    onPrimary = Color.White,
    primaryContainer = AfyaPrimaryContainer,
    onPrimaryContainer = Color(0xFF0A2020),
    secondary = AfyaSecondary,
    onSecondary = Color.White,
    secondaryContainer = AfyaSecondaryContainer,
    onSecondaryContainer = Color(0xFF4A3010),
    tertiary = AfyaTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AfyaTertiaryContainer,
    onTertiaryContainer = Color(0xFF0A2530),
    background = BackgroundLight,
    onBackground = Color(0xFF1A1C1B),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFE4F0EF),
    onSurfaceVariant = Color(0xFF404944),
    error = AfyaError,
    onError = Color.White,
    outline = Color(0xFF6F7975)
)

@Composable
fun AfyaQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
