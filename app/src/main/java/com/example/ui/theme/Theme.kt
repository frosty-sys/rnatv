package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val TonalSpotDarkColorScheme = darkColorScheme(
    primary = TonalDarkPrimary,
    onPrimary = TonalDarkOnPrimary,
    primaryContainer = TonalDarkPrimaryContainer,
    onPrimaryContainer = TonalDarkOnPrimaryContainer,
    secondary = TonalDarkSecondary,
    onSecondary = TonalDarkOnSecondary,
    secondaryContainer = TonalDarkSecondaryContainer,
    onSecondaryContainer = TonalDarkOnSecondaryContainer,
    tertiary = TonalDarkTertiary,
    onTertiary = TonalDarkOnTertiary,
    tertiaryContainer = TonalDarkTertiaryContainer,
    onTertiaryContainer = TonalDarkOnTertiaryContainer,
    background = TonalDarkBackground,
    onBackground = TonalDarkOnBackground,
    surface = TonalDarkSurface,
    onSurface = TonalDarkOnSurface,
    surfaceVariant = TonalDarkSurfaceContainerHigh,
    onSurfaceVariant = TonalDarkOnSurfaceVariant,
    surfaceContainerLowest = TonalDarkSurfaceDim,
    surfaceContainerLow = TonalDarkSurfaceContainerLow,
    surfaceContainer = TonalDarkSurfaceContainer,
    surfaceContainerHigh = TonalDarkSurfaceContainerHigh,
    surfaceContainerHighest = TonalDarkSurfaceContainerHighest,
    outline = TonalDarkOutline,
    outlineVariant = TonalDarkOutlineVariant
)

private val TonalSpotLightColorScheme = lightColorScheme(
    primary = TonalLightPrimary,
    onPrimary = TonalLightOnPrimary,
    primaryContainer = TonalLightPrimaryContainer,
    onPrimaryContainer = TonalLightOnPrimaryContainer,
    secondary = TonalLightSecondary,
    onSecondary = TonalLightOnSecondary,
    secondaryContainer = TonalLightSecondaryContainer,
    onSecondaryContainer = TonalLightOnSecondaryContainer,
    tertiary = TonalLightTertiary,
    onTertiary = TonalLightOnTertiary,
    tertiaryContainer = TonalLightTertiaryContainer,
    onTertiaryContainer = TonalLightOnTertiaryContainer,
    background = TonalLightBackground,
    onBackground = TonalLightOnBackground,
    surface = TonalLightSurface,
    onSurface = TonalLightOnSurface,
    surfaceVariant = TonalLightSurfaceVariant,
    onSurfaceVariant = TonalLightOnSurfaceVariant,
    outline = TonalLightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TonalSpotDarkColorScheme
        else -> TonalSpotLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
