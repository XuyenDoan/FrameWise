package com.framewise.feature.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.framewise.domain.model.GridType

private val LineColor = Color.White.copy(alpha = 0.55f)
private const val LINE_WIDTH_PX = 2f

/**
 * Composition guide grids, drawn purely with Compose `Canvas` — no bitmap
 * assets, so they scale to any preview size/aspect ratio without blurring.
 */
@Composable
fun GridOverlay(gridType: GridType, modifier: Modifier = Modifier) {
    if (gridType == GridType.NONE) return

    Canvas(modifier = modifier) {
        when (gridType) {
            GridType.NONE -> Unit
            GridType.RULE_OF_THIRDS -> drawEvenGrid(columns = 3, rows = 3)
            GridType.GOLDEN_RATIO -> drawGoldenRatio()
            GridType.GOLDEN_TRIANGLE -> drawGoldenTriangle()
            GridType.SQUARE -> drawEvenGrid(columns = 4, rows = 4)
            GridType.DIAGONAL -> drawDiagonals()
        }
    }
}

private fun DrawScope.drawEvenGrid(columns: Int, rows: Int) {
    for (i in 1 until columns) {
        val x = size.width * i / columns
        drawLine(LineColor, Offset(x, 0f), Offset(x, size.height), LINE_WIDTH_PX)
    }
    for (i in 1 until rows) {
        val y = size.height * i / rows
        drawLine(LineColor, Offset(0f, y), Offset(size.width, y), LINE_WIDTH_PX)
    }
}

/** Sections at ~38.2% / 61.8% instead of the thirds' 33.3% / 66.6%. */
private fun DrawScope.drawGoldenRatio() {
    val phiMinor = 0.382f
    val phiMajor = 0.618f
    listOf(phiMinor, phiMajor).forEach { fraction ->
        val x = size.width * fraction
        drawLine(LineColor, Offset(x, 0f), Offset(x, size.height), LINE_WIDTH_PX)
        val y = size.height * fraction
        drawLine(LineColor, Offset(0f, y), Offset(size.width, y), LINE_WIDTH_PX)
    }
}

/** Classic "golden triangle" guide: one main diagonal plus two lines dropped from the opposite corners. */
private fun DrawScope.drawGoldenTriangle() {
    val w = size.width
    val h = size.height

    drawLine(LineColor, Offset(0f, 0f), Offset(w, h), LINE_WIDTH_PX)

    // Foot of the perpendicular from (w, 0) onto the diagonal.
    val t1 = w * w / (w * w + h * h)
    drawLine(LineColor, Offset(w, 0f), Offset(w * t1, h * t1), LINE_WIDTH_PX)

    // Foot of the perpendicular from (0, h) onto the diagonal.
    val t2 = h * h / (w * w + h * h)
    drawLine(LineColor, Offset(0f, h), Offset(w * t2, h * t2), LINE_WIDTH_PX)
}

private fun DrawScope.drawDiagonals() {
    drawLine(LineColor, Offset(0f, 0f), Offset(size.width, size.height), LINE_WIDTH_PX)
    drawLine(LineColor, Offset(size.width, 0f), Offset(0f, size.height), LINE_WIDTH_PX)
}
