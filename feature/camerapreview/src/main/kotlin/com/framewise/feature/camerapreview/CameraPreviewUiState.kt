package com.framewise.feature.camerapreview

import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.GridType
import com.framewise.domain.model.GuidanceType
import com.framewise.domain.model.HorizonState
import com.framewise.domain.model.LensFacing
import com.framewise.domain.model.PoseSuggestion
import com.framewise.domain.model.SceneType

/**
 * Immutable UI state for the camera screen — the single source of truth the
 * Composable reads. Built in the ViewModel by combining every upstream
 * repository Flow (camera/sensor/vision) plus the composition-analysis use
 * case's output, so the UI layer never touches those repositories directly.
 */
data class CameraPreviewUiState(
    val lensFacing: LensFacing = LensFacing.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isTorchAvailable: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val exposureIndex: Int = 0,
    val exposureRange: IntRange = 0..0,
    val isReady: Boolean = false,
    val isCapturing: Boolean = false,
    val gridType: GridType = GridType.RULE_OF_THIRDS,
    val horizonState: HorizonState = HorizonState(),
    val subjects: List<DetectedSubject> = emptyList(),
    val guidanceMessages: List<GuidanceType> = listOf(GuidanceType.GOOD),
    val compositionScore: Int = 100,
    val scene: SceneType = SceneType.UNKNOWN,
    val photographyTip: String = "",
    val isVoiceEnabled: Boolean = false,
    val poseSuggestions: List<PoseSuggestion> = listOf(PoseSuggestion.NONE),
    val horizonLineY: Float? = null,
)

sealed interface CaptureEvent {
    data class Success(val uri: String) : CaptureEvent
    data class Failure(val reason: String) : CaptureEvent
}
