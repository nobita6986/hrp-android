package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = HrpPrimary,
    onPrimary = HrpSurface,
    primaryContainer = HrpPrimaryDark,
    onPrimaryContainer = HrpPrimarySoft,
    secondary = HrpPrimaryDark,
    onSecondary = HrpSurface,
    background = HrpNavyDark,
    onBackground = HrpSurface,
    surface = HrpNavySurface,
    onSurface = HrpSurface,
    surfaceVariant = HrpNavyBorder,
    onSurfaceVariant = HrpTextMuted,
    error = HrpError,
    onError = HrpSurface
)

private val LightColorScheme = lightColorScheme(
    primary = HrpPrimary,
    onPrimary = HrpSurface,
    primaryContainer = HrpPrimarySoft,
    onPrimaryContainer = HrpPrimaryDark,
    secondary = HrpNavyDark,
    onSecondary = HrpSurface,
    background = HrpBackground,
    onBackground = HrpTextPrimary,
    surface = HrpSurface,
    onSurface = HrpTextPrimary,
    surfaceVariant = HrpSurfaceSubtle,
    onSurfaceVariant = HrpTextSecondary,
    error = HrpError,
    onError = HrpSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep HRP branded colors consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
