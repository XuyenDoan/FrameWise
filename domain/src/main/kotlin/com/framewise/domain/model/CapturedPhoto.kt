package com.framewise.domain.model

data class CapturedPhoto(
    val filePath: String,
    val capturedAtEpochMillis: Long,
)

sealed interface CameraCaptureResult {
    data class Success(val photo: CapturedPhoto) : CameraCaptureResult
    data class Failure(val reason: String) : CameraCaptureResult
}
