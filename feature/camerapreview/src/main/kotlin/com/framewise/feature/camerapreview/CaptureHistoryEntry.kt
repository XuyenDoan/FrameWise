package com.framewise.feature.camerapreview

/**
 * One capture's outcome, kept in-memory for the lifetime of the camera
 * screen's ViewModel so the user can compare recent attempts ("before/
 * after") without a database — the app doesn't need this history to
 * survive process death, only the current session.
 */
data class CaptureHistoryEntry(
    val uri: String,
    val compositionScore: Int,
    val capturedAtEpochMillis: Long,
)
