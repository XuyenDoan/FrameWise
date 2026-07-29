package com.framewise.data.vision

import androidx.camera.core.ImageProxy

/**
 * Estimates where the horizon sits *within the frame* (top-to-bottom
 * fraction) from the Y (luma) plane alone — sky-to-ground is usually the
 * single strongest brightness edge in an outdoor scene, so this scans for
 * the row with the biggest jump in average row luminance, gated by a
 * confidence check against the whole frame's luminance range so it stays
 * `null` (rather than guessing) for indoor scenes, overcast skies, or
 * anything without a clear split.
 *
 * This is a cheap heuristic, not a trained line/segmentation model — it
 * will misfire on scenes with a strong non-horizon edge (e.g. a wall
 * meeting the floor). It is also **not** the horizon *tilt* — see
 * [com.framewise.domain.model.HorizonState], which comes from the device's
 * rotation sensor instead.
 *
 * KNOWN LIMITATION shared with [LuminanceEvaluator]: this scans the raw
 * Y-plane buffer by [ImageProxy.getWidth]/[ImageProxy.getHeight], which are
 * in sensor orientation, not the rotation-corrected orientation ML Kit's
 * bounding boxes use elsewhere in this pipeline. On a phone held in
 * portrait (the common case), that means this detector is effectively
 * scanning the wrong axis. Fixing this needs the same rotation handling
 * `MlKitFrameAnalyzer` already does for `InputImage.fromMediaImage`
 * applied here too — flagged rather than silently shipped as correct,
 * since it wasn't possible to verify against a real device in the
 * environment this was written in.
 */
internal object HorizonLineDetector {

    private const val ROW_SAMPLE_STRIDE = 4
    private const val COLUMN_SAMPLE_STRIDE = 8
    private const val EDGE_MARGIN_FRACTION = 0.05f
    private const val MIN_CONFIDENCE_GRADIENT = 18f

    fun detect(imageProxy: ImageProxy): Float? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        val topMargin = (height * EDGE_MARGIN_FRACTION).toInt()
        val bottomMargin = height - topMargin
        if (bottomMargin <= topMargin) return null

        val rowMeans = ArrayList<Float>((height / ROW_SAMPLE_STRIDE) + 1)
        val rowIndices = ArrayList<Int>(rowMeans.size)

        var y = 0
        while (y < height) {
            var sum = 0L
            var count = 0
            val rowOffset = y * rowStride
            var x = 0
            while (x < width) {
                val index = rowOffset + x * pixelStride
                if (index in 0 until buffer.capacity()) {
                    sum += buffer.get(index).toInt() and 0xFF
                    count++
                }
                x += COLUMN_SAMPLE_STRIDE
            }
            if (count > 0) {
                rowMeans += sum.toFloat() / count
                rowIndices += y
            }
            y += ROW_SAMPLE_STRIDE
        }

        if (rowMeans.size < 3) return null

        var bestGradient = 0f
        var bestRowIndex = -1
        for (i in 1 until rowMeans.size) {
            val row = rowIndices[i]
            if (row < topMargin || row > bottomMargin) continue
            val gradient = kotlin.math.abs(rowMeans[i] - rowMeans[i - 1])
            if (gradient > bestGradient) {
                bestGradient = gradient
                bestRowIndex = row
            }
        }

        if (bestRowIndex < 0 || bestGradient < MIN_CONFIDENCE_GRADIENT) return null

        return (bestRowIndex.toFloat() / height).coerceIn(0f, 1f)
    }
}
