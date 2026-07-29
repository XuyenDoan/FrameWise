package com.framewise.data.vision.segmentation

import com.framewise.domain.model.BackgroundState
import java.nio.ByteBuffer

/**
 * Estimates background clutter purely from the shape of the segmentation
 * category mask (how ragged the foreground/background boundary is), by
 * counting foreground/background transitions per sampled row. A clean
 * background typically leaves the person as one contiguous blob (few
 * transitions per row); a visually busy background tends to confuse the
 * segmenter into a more fragmented mask (more transitions).
 *
 * This is an indirect proxy for "busy background", not a direct measure of
 * background texture/clutter — deliberately kept this way (rather than
 * cross-referencing the mask against the raw luma-plane pixels) to avoid
 * combining two different coordinate spaces; see
 * [BackgroundSegmentationProcessor]'s KDoc for why.
 */
internal object BackgroundClutterScorer {

    private const val ROW_SAMPLE_STRIDE = 6
    private const val BUSY_AVG_TRANSITIONS_PER_ROW = 6f

    fun score(maskBuffer: ByteBuffer, width: Int, height: Int): BackgroundState {
        if (width <= 0 || height <= 0) return BackgroundState.UNKNOWN

        var totalTransitions = 0
        var rowCount = 0
        var y = 0
        while (y < height) {
            val rowOffset = y * width
            var previousIsForeground: Boolean? = null
            var x = 0
            while (x < width) {
                val index = rowOffset + x
                if (index !in 0 until maskBuffer.capacity()) break
                val isForeground = maskBuffer.get(index).toInt() != 0
                if (previousIsForeground != null && previousIsForeground != isForeground) {
                    totalTransitions++
                }
                previousIsForeground = isForeground
                x++
            }
            rowCount++
            y += ROW_SAMPLE_STRIDE
        }

        if (rowCount == 0) return BackgroundState.UNKNOWN
        val averageTransitions = totalTransitions.toFloat() / rowCount
        return if (averageTransitions > BUSY_AVG_TRANSITIONS_PER_ROW) {
            BackgroundState.BUSY
        } else {
            BackgroundState.CLEAN
        }
    }
}
