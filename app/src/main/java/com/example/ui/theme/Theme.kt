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
    primary = SatElectricBlueLight,
    onPrimary = Color.White,
    primaryContainer = SatNavyElevated,
    onPrimaryContainer = SatSlate100,
    secondary = SatTeal,
    onSecondary = SatNavyDark,
    secondaryContainer = SatTealDark,
    onSecondaryContainer = SatTealContainer,
    background = SatNavyDark,
    onBackground = SatSlate100,
    surface = SatNavySurface,
    onSurface = SatSlate100,
    surfaceVariant = SatNavyElevated,
    onSurfaceVariant = SatSlate400,
    outline = SatNavyBorder,
    error = SatErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = SatElectricBlue,
    onPrimary = Color.White,
    primaryContainer = SatElectricBlueContainer,
    onPrimaryContainer = SatNavy,
    secondary = SatTealDark,
    onSecondary = Color.White,
    secondaryContainer = SatTealContainer,
    onSecondaryContainer = SatNavy,
    background = SatSlate50,
    onBackground = SatSlate900,
    surface = Color.White,
    onSurface = SatSlate900,
    surfaceVariant = SatSlate100,
    onSurfaceVariant = SatSlate600,
    outline = SatSlate200,
    error = SatErrorRed
)

@Composable
fun SATMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep corporate SAT branding consistent
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
