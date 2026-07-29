package com.framewise.feature.camerapreview

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.framewise.data.camera.CameraPreviewBinder
import com.framewise.domain.model.CameraCaptureResult
import com.framewise.domain.model.GridType
import com.framewise.domain.repository.CameraRepository
import com.framewise.domain.repository.SensorRepository
import com.framewise.domain.repository.VisionRepository
import com.framewise.domain.usecase.AnalyzeCompositionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CameraPreviewViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val previewBinder: CameraPreviewBinder,
    private val sensorRepository: SensorRepository,
    private val visionRepository: VisionRepository,
    private val analyzeComposition: AnalyzeCompositionUseCase,
) : ViewModel() {

    private val isCapturing = MutableStateFlow(false)
    private val selectedGridType = MutableStateFlow(GridType.RULE_OF_THIRDS)

    val uiState: StateFlow<CameraPreviewUiState> = combine(
        cameraRepository.cameraState,
        sensorRepository.horizonState,
        visionRepository.visionResults,
        selectedGridType,
        isCapturing,
    ) { cameraState, horizonState, visionResult, gridType, capturing ->
        val guidance = analyzeComposition(vision = visionResult, horizon = horizonState)

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
            isCapturing = capturing,
            gridType = gridType,
            horizonState = horizonState,
            subjects = visionResult.subjects,
            guidanceMessages = guidance.messages,
            compositionScore = guidance.score,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CameraPreviewUiState(),
        )

    private val _captureEvents = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 1)
    val captureEvents: SharedFlow<CaptureEvent> = _captureEvents

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

    fun onCapture() {
        if (isCapturing.value) return
        viewModelScope.launch {
            isCapturing.value = true
            when (val result = cameraRepository.capturePhoto()) {
                is CameraCaptureResult.Success -> _captureEvents.emit(CaptureEvent.Success(result.photo.filePath))
                is CameraCaptureResult.Failure -> _captureEvents.emit(CaptureEvent.Failure(result.reason))
            }
            isCapturing.value = false
        }
    }
}
