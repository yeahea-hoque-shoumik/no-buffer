package com.prime.browser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ChromeLightColorScheme = lightColorScheme(
    primary = ChromeAccentLight,
    onPrimary = ChromeToolbarLight,
    surface = ChromeSurfaceLight,
    onSurface = ChromePrimaryTextLight,
    surfaceVariant = ChromeOmniboxLight,
    onSurfaceVariant = ChromeSecondaryTextLight,
    background = ChromeSurfaceLight,
    onBackground = ChromePrimaryTextLight,
    outline = ChromeDividerLight,
)

private val ChromeDarkColorScheme = darkColorScheme(
    primary = ChromeAccentDark,
    onPrimary = ChromeToolbarDark,
    surface = ChromeSurfaceDark,
    onSurface = ChromePrimaryTextDark,
    surfaceVariant = ChromeOmniboxDark,
    onSurfaceVariant = ChromeSecondaryTextDark,
    background = ChromeSurfaceDark,
    onBackground = ChromePrimaryTextDark,
    outline = ChromeDividerDark,
)

private val IncognitoColorScheme = darkColorScheme(
    primary = IncognitoOnSurface,
    onPrimary = IncognitoSurface,
    surface = IncognitoSurface,
    onSurface = IncognitoOnSurface,
    surfaceVariant = IncognitoSurface,
    onSurfaceVariant = IncognitoSecondary,
    background = IncognitoSurface,
    onBackground = IncognitoOnSurface,
)

@Composable
fun BrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ChromeDarkColorScheme else ChromeLightColorScheme
    val orionColors = if (darkTheme) OrionTokens.Dark else OrionTokens.Light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = orionColors.bg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalOrionColors provides orionColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun IncognitoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = IncognitoSurface.toArgb()
            window.navigationBarColor = IncognitoSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    CompositionLocalProvider(LocalOrionColors provides OrionTokens.Incognito) {
        MaterialTheme(
            colorScheme = IncognitoColorScheme,
            typography = Typography,
            content = content
        )
    }
}
