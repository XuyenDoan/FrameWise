package com.framewise.domain.model

/**
 * One of MediaPipe Pose's 33 body landmarks, in normalized (0f..1f) image
 * coordinates. Indices follow MediaPipe's standard topology (0 = nose,
 * 11/12 = left/right shoulder, etc.) — see [AnalyzePoseUseCase][com.framewise.domain.usecase.AnalyzePoseUseCase]
 * for the indices actually used.
 */
data class PoseLandmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
)
