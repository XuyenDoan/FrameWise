package com.framewise.feature.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.framewise.domain.model.HorizonLevel
import com.framewise.domain.model.HorizonState
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dedicated-camera-style level indicator (Sony/Fuji/Leica): a static center
 * tick plus a line that rotates with device roll, turning green once level.
 * Deliberately not a full artificial-horizon widget — a photographer only
 * needs "am I level yet", not pitch/roll telemetry.
 */
@Composable
fun HorizonLevelOverlay(horizonState: HorizonState, modifier: Modifier = Modifier) {
    val indicatorColor = when (horizonState.level) {
        HorizonLevel.LEVEL -> Color(0xFF3DDC84)
        else -> Color.White
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .width(120.dp)
                .height(120.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val halfLineLength = size.width * 0.35f

            // Static reference tick — always horizontal.
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(center.x - halfLineLength, center.y),
                end = Offset(center.x + halfLineLength, center.y),
                strokeWidth = 2f,
            )

            // Rotating line following the device's roll angle.
            rotate(degrees = horizonState.angleDegrees, pivot = center) {
                drawLine(
                    color = indicatorColor,
                    start = Offset(center.x - halfLineLength, center.y),
                    end = Offset(center.x + halfLineLength, center.y),
                    strokeWidth = 4f,
                )
            }

            drawCircle(color = indicatorColor, radius = 4f, center = center, style = Stroke(width = 2f))
        }

        if (horizonState.level != HorizonLevel.LEVEL) {
            val direction = if (horizonState.angleDegrees > 0) "Xoay trái" else "Xoay phải"
            val degrees = abs(horizonState.angleDegrees).roundToInt()
            Text(
                text = "$direction $degrees°",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
