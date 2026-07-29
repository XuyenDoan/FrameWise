package com.framewise.domain.model

/**
 * A bounding box expressed as fractions (0f..1f) of the frame's
 * width/height, so it's independent of any actual pixel resolution or
 * screen size — the UI layer scales it to whatever canvas it draws on.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}
