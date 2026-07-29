package com.framewise.domain.repository

import com.framewise.domain.model.CameraCaptureResult
import com.framewise.domain.model.CameraState
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.LensFacing
import com.framewise.domain.model.Resolution
import kotlinx.coroutines.flow.StateFlow

/**
 * Control/state contract for the camera, deliberately free of any CameraX
 * or Android UI type so ViewModels depending on it stay unit-testable with
 * a fake implementation.
 *
 * Binding the live preview surface (which inherently requires an
 * Android `PreviewView`/`LifecycleOwner`) is NOT part of this contract on
 * purpose — see `data/camera`'s `CameraPreviewBinder` for that narrow,
 * documented exception to the domain-purity rule.
 */
interface CameraRepository {
    val cameraState: StateFlow<CameraState>

    fun setLensFacing(lensFacing: LensFacing)
    fun setFlashMode(flashMode: FlashMode)
    fun setZoomRatio(ratio: Float)
    fun setExposureIndex(index: Int)
    /** Must be one of [CameraState.availableResolutions] - picking anything else is a no-op. */
    fun setResolution(resolution: Resolution)

    suspend fun capturePhoto(): CameraCaptureResult
}
