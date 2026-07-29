package com.framewise.feature.overlay

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.framewise.domain.model.GuidanceType
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single directional chevron pointing the way to fix the current top
 * guidance message, pulsing gently so it reads as "keep going this way"
 * rather than a static decoration. Returns nothing for [GuidanceType.GOOD]
 * since there's nothing left to point at.
 */
@Composable
fun ArGuidanceArrow(guidance: GuidanceType, modifier: Modifier = Modifier) {
    if (guidance == GuidanceType.GOOD) return

    val transition = rememberInfiniteTransition(label = "ar-guidance-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Canvas(modifier = modifier.size(56.dp)) {
        val color = Color(0xFFFFB300).copy(alpha = pulse)
        val center = Offset(size.width / 2f, size.height / 2f)

        when (guidance) {
            GuidanceType.MOVE_LEFT -> rotate(180f, center) { drawChevron(color) }
            GuidanceType.MOVE_RIGHT -> drawChevron(color)
            GuidanceType.RAISE_CAMERA -> rotate(-90f, center) { drawChevron(color) }
            GuidanceType.LOWER_CAMERA -> rotate(90f, center) { drawChevron(color) }
            GuidanceType.MOVE_CLOSER -> drawConvergingChevrons(color)
            GuidanceType.MOVE_FARTHER -> drawDivergingChevrons(color)
            GuidanceType.LEVEL_HORIZON -> drawRotateArc(color)
            GuidanceType.IMPROVE_LIGHTING, GuidanceType.GOOD -> Unit
        }
    }
}

private fun DrawScope.drawChevron(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.35f, h * 0.2f)
        lineTo(w * 0.75f, h * 0.5f)
        lineTo(w * 0.35f, h * 0.8f)
    }
    drawPath(path, color = color, style = Stroke(width = 8f, cap = StrokeCap.Round))
}

private fun DrawScope.drawConvergingChevrons(color: Color) {
    val w = size.width
    val h = size.height
    val left = Path().apply {
        moveTo(w * 0.15f, h * 0.2f)
        lineTo(w * 0.5f, h * 0.5f)
        lineTo(w * 0.15f, h * 0.8f)
    }
    val right = Path().apply {
        moveTo(w * 0.85f, h * 0.2f)
        lineTo(w * 0.5f, h * 0.5f)
        lineTo(w * 0.85f, h * 0.8f)
    }
    val stroke = Stroke(width = 8f, cap = StrokeCap.Round)
    drawPath(left, color = color, style = stroke)
    drawPath(right, color = color, style = stroke)
}

private fun DrawScope.drawDivergingChevrons(color: Color) {
    val w = size.width
    val h = size.height
    val left = Path().apply {
        moveTo(w * 0.5f, h * 0.2f)
        lineTo(w * 0.15f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.8f)
    }
    val right = Path().apply {
        moveTo(w * 0.5f, h * 0.2f)
        lineTo(w * 0.85f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.8f)
    }
    val stroke = Stroke(width = 8f, cap = StrokeCap.Round)
    drawPath(left, color = color, style = stroke)
    drawPath(right, color = color, style = stroke)
}

private fun DrawScope.drawRotateArc(color: Color) {
    val strokeWidth = 6f
    val radius = size.minDimension * 0.32f
    val center = Offset(size.width / 2f, size.height / 2f)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)

    drawArc(
        color = color,
        startAngle = -220f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    val headAngleRad = Math.toRadians(40.0)
    val headTip = Offset(
        x = center.x + radius * cos(headAngleRad).toFloat(),
        y = center.y + radius * sin(headAngleRad).toFloat(),
    )
    val headPath = Path().apply {
        moveTo(headTip.x - 10f, headTip.y - 6f)
        lineTo(headTip.x + 6f, headTip.y)
        lineTo(headTip.x - 6f, headTip.y + 10f)
    }
    drawPath(headPath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}
