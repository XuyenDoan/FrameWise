package com.framewise.data.camera

import android.content.Context
import android.os.Environment
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.framewise.domain.model.CameraCaptureResult
import com.framewise.domain.model.CameraState
import com.framewise.domain.model.CapturedPhoto
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.LensFacing
import com.framewise.domain.repository.CameraRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Single owner of the CameraX `ProcessCameraProvider` and its use cases.
 * Implements both the [CameraRepository] control contract and the
 * [CameraPreviewBinder] surface-wiring contract (see that file for why
 * they're split).
 *
 * All CameraX binding calls happen on the main thread, as required by the
 * CameraX API; state changes are still safe to observe from any thread via
 * [cameraState].
 */
@Singleton
class CameraXController @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraRepository, CameraPreviewBinder {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _cameraState = MutableStateFlow(CameraState())
    override val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private var lensFacing = LensFacing.BACK
    private var flashMode = FlashMode.OFF

    override fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView

        controllerScope.launch {
            val provider = awaitCameraProvider()
            cameraProvider = provider
            preview.setSurfaceProvider(previewView.surfaceProvider)
            rebindUseCases(provider, lifecycleOwner)
        }
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
        camera = null
        lifecycleOwner = null
        previewView = null
        _cameraState.value = _cameraState.value.copy(isReady = false)
    }

    override fun setLensFacing(lensFacing: LensFacing) {
        this.lensFacing = lensFacing
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        controllerScope.launch { rebindUseCases(provider, owner) }
    }

    override fun setFlashMode(flashMode: FlashMode) {
        this.flashMode = flashMode
        imageCapture.flashMode = when (flashMode) {
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.OFF, FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
        }
        camera?.cameraControl?.enableTorch(flashMode == FlashMode.TORCH)
        _cameraState.value = _cameraState.value.copy(flashMode = flashMode)
    }

    override fun setZoomRatio(ratio: Float) {
        val state = _cameraState.value
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(clamped)
        _cameraState.value = state.copy(zoomRatio = clamped)
    }

    override fun setExposureIndex(index: Int) {
        val state = _cameraState.value
        val clamped = index.coerceIn(state.exposureRange.first, state.exposureRange.last)
        camera?.cameraControl?.setExposureCompensationIndex(clamped)
        _cameraState.value = state.copy(exposureIndex = clamped)
    }

    override suspend fun capturePhoto(): CameraCaptureResult {
        val outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        val photoFile = File(outputDirectory, "FrameWise_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        return suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(
                            CameraCaptureResult.Success(
                                CapturedPhoto(
                                    filePath = photoFile.absolutePath,
                                    capturedAtEpochMillis = System.currentTimeMillis(),
                                ),
                            ),
                        )
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(
                            CameraCaptureResult.Failure(exception.message ?: "Unknown capture error"),
                        )
                    }
                },
            )
        }
    }

    private fun rebindUseCases(provider: ProcessCameraProvider, owner: LifecycleOwner) {
        val cameraSelector = when (lensFacing) {
            LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        provider.unbindAll()
        val boundCamera = provider.bindToLifecycle(owner, cameraSelector, preview, imageCapture)
        camera = boundCamera

        val cameraInfo = boundCamera.cameraInfo
        val zoomState = cameraInfo.zoomState.value
        val exposureState = cameraInfo.exposureState

        _cameraState.value = CameraState(
            lensFacing = lensFacing,
            flashMode = flashMode,
            isTorchAvailable = cameraInfo.hasFlashUnit(),
            zoomRatio = zoomState?.zoomRatio ?: 1f,
            minZoomRatio = zoomState?.minZoomRatio ?: 1f,
            maxZoomRatio = zoomState?.maxZoomRatio ?: 1f,
            exposureIndex = exposureState.exposureCompensationIndex,
            exposureRange = exposureState.exposureCompensationRange.lower..exposureState.exposureCompensationRange.upper,
            isReady = true,
        )
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (error: Exception) {
                        continuation.resumeWithException(error)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
}
