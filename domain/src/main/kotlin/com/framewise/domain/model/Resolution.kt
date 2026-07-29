package com.framewise.domain.model

/**
 * A still-capture resolution in pixels, as reported by the device's own
 * camera hardware (via [com.framewise.domain.repository.CameraRepository]'s
 * [CameraState.availableResolutions]) - never an app-invented/guessed value.
 */
data class Resolution(val width: Int, val height: Int) {
    val megapixels: Float get() = (width.toLong() * height) / 1_000_000f
}
