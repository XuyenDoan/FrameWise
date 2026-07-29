package com.framewise.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette inspired by Sony/Fujifilm/Leica viewfinder UIs: near-black
 * background so the live preview stays the visual focus, a single warm
 * accent for active/selected state, and semantic colors reserved for the
 * horizon level and guidance indicators only.
 */
object FrameWiseColors {
    val Background = Color(0xFF0B0B0C)
    val Surface = Color(0xFF1A1A1C)
    val SurfaceVariant = Color(0xFF2A2A2D)
    val OnSurface = Color(0xFFF2F2F2)
    val OnSurfaceMuted = Color(0xFFA0A0A3)

    val Accent = Color(0xFFFFB300)
    val OnAccent = Color(0xFF1A1A1C)

    val LevelGood = Color(0xFF3DDC84)
    val LevelWarning = Color(0xFFFFB300)
    val LevelBad = Color(0xFFFF5252)

    val OverlayScrim = Color(0x66000000)
}
