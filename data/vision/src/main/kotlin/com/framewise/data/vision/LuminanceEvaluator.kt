package com.framewise.data.vision

import androidx.camera.core.ImageProxy
import com.framewise.domain.model.LightingState
import com.framewise.domain.model.NormalizedRect

/**
 * Cheap exposure/backlight check computed directly from the Y (luma) plane
 * of the YUV_420_888 analysis frame — no ML model needed, so it costs
 * effectively nothing per frame. Samples on a stride rather than every
 * pixel; frame-to-frame precision doesn't matter for this, only the
 * ballpark bucket (under/over/backlit/good).
 */
internal object LuminanceEvaluator {

    private const val SAMPLE_STRIDE = 8
    private const val UNDEREXPOSED_THRESHOLD = 60
    private const val OVEREXPOSED_THRESHOLD = 200
    private const val BACKLIT_DELTA = 45

    fun evaluate(imageProxy: ImageProxy, primarySubjectBox: NormalizedRect?): LightingState {
        val plane = imageProxy.planes.firstOrNull() ?: return LightingState.GOOD
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        val overallMean = meanLuma(buffer, rowStride, pixelStride, width, height, 0f, 0f, 1f, 1f)

        val subjectMean = primarySubjectBox?.let { box ->
            meanLuma(buffer, rowStride, pixelStride, width, height, box.left, box.top, box.right, box.bottom)
        }

        return when {
            subjectMean != null && overallMean - subjectMean > BACKLIT_DELTA -> LightingState.BACKLIT
            overallMean < UNDEREXPOSED_THRESHOLD -> LightingState.UNDEREXPOSED
            overallMean > OVEREXPOSED_THRESHOLD -> LightingState.OVEREXPOSED
            else -> LightingState.GOOD
        }
    }

    private fun meanLuma(
        buffer: java.nio.ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        width: Int,
        height: Int,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float,
    ): Float {
        val startX = (leftFraction * width).toInt().coerceIn(0, width - 1)
        val endX = (rightFraction * width).toInt().coerceIn(startX + 1, width)
        val startY = (topFraction * height).toInt().coerceIn(0, height - 1)
        val endY = (bottomFraction * height).toInt().coerceIn(startY + 1, height)

        var sum = 0L
        var count = 0
        var y = startY
        while (y < endY) {
            var x = startX
            val rowOffset = y * rowStride
            while (x < endX) {
                val index = rowOffset + x * pixelStride
                if (index in 0 until buffer.capacity()) {
                    sum += buffer.get(index).toInt() and 0xFF
                    count++
                }
                x += SAMPLE_STRIDE
            }
            y += SAMPLE_STRIDE
        }

        return if (count == 0) 128f else sum.toFloat() / count
    }
}
