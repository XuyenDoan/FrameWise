package com.framewise.domain.model

/**
 * Platform-agnostic snapshot of the camera's controllable state. Produced by
 * [com.framewise.domain.repository.CameraRepository] so the ViewModel layer
 * never needs to know CameraX exists.
 */
data class CameraState(
    val lensFacing: LensFacing = LensFacing.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isTorchAvailable: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val exposureIndex: Int = 0,
    val exposureRange: IntRange = 0..0,
    val isReady: Boolean = false,
)
