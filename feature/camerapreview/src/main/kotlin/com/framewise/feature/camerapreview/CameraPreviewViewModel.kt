package com.framewise.feature.camerapreview

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.framewise.data.camera.CameraPreviewBinder
import com.framewise.domain.model.CameraCaptureResult
import com.framewise.domain.model.CameraState
import com.framewise.domain.model.GridType
import com.framewise.domain.model.GuidanceType
import com.framewise.domain.model.HorizonState
import com.framewise.domain.model.VisionResult
import com.framewise.domain.repository.CameraRepository
import com.framewise.domain.repository.SensorRepository
import com.framewise.domain.repository.VisionRepository
import com.framewise.domain.usecase.AnalyzeCompositionUseCase
import com.framewise.domain.usecase.GetPhotographyTipsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CameraPreviewViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val previewBinder: CameraPreviewBinder,
    private val sensorRepository: SensorRepository,
    private val visionRepository: VisionRepository,
    private val analyzeComposition: AnalyzeCompositionUseCase,
    private val getPhotographyTips: GetPhotographyTipsUseCase,
    private val voiceSpeaker: VoiceGuidanceSpeaker,
) : ViewModel() {

    private val isCapturing = MutableStateFlow(false)
    private val selectedGridType = MutableStateFlow(GridType.RULE_OF_THIRDS)
    private val isVoiceEnabled = MutableStateFlow(false)

    private data class CoreState(
        val cameraState: CameraState,
        val horizonState: HorizonState,
        val visionResult: VisionResult,
        val gridType: GridType,
        val capturing: Boolean,
    )

    private val coreState = combine(
        cameraRepository.cameraState,
        sensorRepository.horizonState,
        visionRepository.visionResults,
        selectedGridType,
        isCapturing,
    ) { cameraState, horizonState, visionResult, gridType, capturing ->
        CoreState(cameraState, horizonState, visionResult, gridType, capturing)
    }

    val uiState: StateFlow<CameraPreviewUiState> = combine(coreState, isVoiceEnabled) { core, voiceEnabled ->
        val guidance = analyzeComposition(vision = core.visionResult, horizon = core.horizonState)
        val cameraState = core.cameraState

        CameraPreviewUiState(
            lensFacing = cameraState.lensFacing,
            flashMode = cameraState.flashMode,
            isTorchAvailable = cameraState.isTorchAvailable,
            zoomRatio = cameraState.zoomRatio,
            minZoomRatio = cameraState.minZoomRatio,
            maxZoomRatio = cameraState.maxZoomRatio,
            exposureIndex = cameraState.exposureIndex,
            exposureRange = cameraState.exposureRange,
            isReady = cameraState.isReady,
            isCapturing = core.capturing,
            gridType = core.gridType,
            horizonState = core.horizonState,
            subjects = core.visionResult.subjects,
            guidanceMessages = guidance.messages,
            compositionScore = guidance.score,
            scene = core.visionResult.scene,
            photographyTip = getPhotographyTips(core.visionResult.scene),
            isVoiceEnabled = voiceEnabled,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CameraPreviewUiState(),
        )

    private val _captureEvents = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 1)
    val captureEvents: SharedFlow<CaptureEvent> = _captureEvents

    private val _captureHistory = MutableStateFlow<List<CaptureHistoryEntry>>(emptyList())
    val captureHistory: StateFlow<List<CaptureHistoryEntry>> = _captureHistory

    init {
        viewModelScope.launch {
            uiState
                .map { it.isVoiceEnabled to (it.guidanceMessages.minByOrNull { g -> g.priority } ?: GuidanceType.GOOD) }
                .distinctUntilChanged()
                .collect { (enabled, topGuidance) ->
                    if (enabled) {
                        voiceSpeaker.speak(topGuidance.toSpokenVietnamese())
                    }
                }
        }
    }

    fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        previewBinder.bind(lifecycleOwner, previewView)
    }

    fun unbindPreview() {
        previewBinder.unbind()
    }

    fun onToggleLens() {
        cameraRepository.setLensFacing(uiState.value.lensFacing.opposite())
    }

    fun onToggleFlash() {
        cameraRepository.setFlashMode(uiState.value.flashMode.next())
    }

    fun onZoomChange(ratio: Float) {
        cameraRepository.setZoomRatio(ratio)
    }

    fun onExposureChange(index: Int) {
        cameraRepository.setExposureIndex(index)
    }

    fun onFocusTap(x: Float, y: Float) {
        previewBinder.focusAt(x, y)
    }

    fun onGridTypeSelected(gridType: GridType) {
        selectedGridType.value = gridType
    }

    fun onToggleVoice() {
        isVoiceEnabled.value = !isVoiceEnabled.value
    }

    fun onCapture() {
        if (isCapturing.value) return
        viewModelScope.launch {
            isCapturing.value = true
            val scoreAtCapture = uiState.value.compositionScore
            when (val result = cameraRepository.capturePhoto()) {
                is CameraCaptureResult.Success -> {
                    _captureHistory.value = listOf(
                        CaptureHistoryEntry(
                            filePath = result.photo.filePath,
                            compositionScore = scoreAtCapture,
                            capturedAtEpochMillis = result.photo.capturedAtEpochMillis,
                        ),
                    ) + _captureHistory.value
                    _captureEvents.emit(CaptureEvent.Success(result.photo.filePath))
                }
                is CameraCaptureResult.Failure -> _captureEvents.emit(CaptureEvent.Failure(result.reason))
            }
            isCapturing.value = false
        }
    }

    override fun onCleared() {
        voiceSpeaker.shutdown()
        super.onCleared()
    }
}

private fun GuidanceType.toSpokenVietnamese(): String = when (this) {
    GuidanceType.LEVEL_HORIZON -> "Hãy cân bằng đường chân trời"
    GuidanceType.MOVE_LEFT -> "Di chuyển sang trái"
    GuidanceType.MOVE_RIGHT -> "Di chuyển sang phải"
    GuidanceType.RAISE_CAMERA -> "Nâng camera lên"
    GuidanceType.LOWER_CAMERA -> "Hạ camera xuống"
    GuidanceType.MOVE_CLOSER -> "Hãy tiến thêm một bước"
    GuidanceType.MOVE_FARTHER -> "Hãy lùi ra xa hơn"
    GuidanceType.IMPROVE_LIGHTING -> "Điều chỉnh lại ánh sáng"
    GuidanceType.GOOD -> "Bố cục đẹp, có thể chụp"
}
