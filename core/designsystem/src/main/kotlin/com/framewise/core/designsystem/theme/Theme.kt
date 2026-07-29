package com.framewise.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FrameWiseDarkColorScheme = darkColorScheme(
    background = FrameWiseColors.Background,
    surface = FrameWiseColors.Surface,
    surfaceVariant = FrameWiseColors.SurfaceVariant,
    onBackground = FrameWiseColors.OnSurface,
    onSurface = FrameWiseColors.OnSurface,
    onSurfaceVariant = FrameWiseColors.OnSurfaceMuted,
    primary = FrameWiseColors.Accent,
    onPrimary = FrameWiseColors.OnAccent,
)

private val FrameWiseLightColorScheme = lightColorScheme(
    primary = FrameWiseColors.Accent,
    onPrimary = FrameWiseColors.OnAccent,
)

/**
 * The camera screen always forces [darkTheme] = true regardless of system
 * setting — matching Sony/Fuji/Leica viewfinder conventions where a light
 * UI would wash out next to the live preview. Non-camera screens (settings,
 * tips) may follow the system theme.
 */
@Composable
fun FrameWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) FrameWiseDarkColorScheme else FrameWiseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FrameWiseTypography,
        content = content,
    )
}
