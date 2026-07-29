package com.framewise.domain.model

/** [uri] is a content:// MediaStore URI (as a String) - the photo is saved
 * into the device's public Pictures/FrameWise, not app-private storage, so
 * it shows up in the Gallery/Photos app like any other camera shot. */
data class CapturedPhoto(
    val uri: String,
    val capturedAtEpochMillis: Long,
)

sealed interface CameraCaptureResult {
    data class Success(val photo: CapturedPhoto) : CameraCaptureResult
    data class Failure(val reason: String) : CameraCaptureResult
}
