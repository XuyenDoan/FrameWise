package com.framewise.feature.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect

/**
 * Thin dashed line at the *detected* horizon position (computer-vision
 * estimate of where sky meets ground in the current frame) — distinct
 * from [HorizonLevelOverlay], which shows the sensor-based tilt. Draws
 * nothing when no horizon was confidently detected.
 */
@Composable
fun HorizonLineOverlay(horizonLineY: Float?, modifier: Modifier = Modifier) {
    if (horizonLineY == null) return

    Canvas(modifier = modifier) {
        val y = size.height * horizonLineY
        drawLine(
            color = Color(0xFF3DDC84).copy(alpha = 0.7f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f),
        )
    }
}
