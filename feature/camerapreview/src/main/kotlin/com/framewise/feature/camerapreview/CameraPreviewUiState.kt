package com.framewise.feature.camerapreview

import com.framewise.domain.model.CameraState
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.LensFacing

/**
 * Immutable UI state for the camera screen. A 1:1 mapping of
 * [CameraState] today; kept as its own type (rather than reusing the
 * domain model directly) so UI-only fields (e.g. [isCapturing]) can be
 * added later without leaking into domain.
 */
data class CameraPreviewUiState(
    val lensFacing: LensFacing = LensFacing.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isTorchAvailable: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val isReady: Boolean = false,
    val isCapturing: Boolean = false,
)

internal fun CameraState.toUiState(isCapturing: Boolean = false) = CameraPreviewUiState(
    lensFacing = lensFacing,
    flashMode = flashMode,
    isTorchAvailable = isTorchAvailable,
    zoomRatio = zoomRatio,
    minZoomRatio = minZoomRatio,
    maxZoomRatio = maxZoomRatio,
    isReady = isReady,
    isCapturing = isCapturing,
)

sealed interface CaptureEvent {
    data class Success(val filePath: String) : CaptureEvent
    data class Failure(val reason: String) : CaptureEvent
}
