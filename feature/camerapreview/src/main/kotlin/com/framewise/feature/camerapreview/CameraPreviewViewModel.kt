package com.framewise.feature.camerapreview

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.framewise.data.camera.CameraPreviewBinder
import com.framewise.domain.model.CameraCaptureResult
import com.framewise.domain.repository.CameraRepository
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
) : ViewModel() {

    private val isCapturing = MutableStateFlow(false)

    val uiState: StateFlow<CameraPreviewUiState> = combine(
        cameraRepository.cameraState,
        isCapturing,
    ) { cameraState, capturing -> cameraState.toUiState(isCapturing = capturing) }
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
