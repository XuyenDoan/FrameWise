package com.framewise.data.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.framewise.domain.model.CameraCaptureResult
import com.framewise.domain.model.CameraState
import com.framewise.domain.model.CapturedPhoto
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.LensFacing
import com.framewise.domain.model.Resolution
import com.framewise.domain.repository.CameraRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
) : CameraRepository, CameraPreviewBinder, CameraFrameProvider {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableStateFlow(CameraState())
    override val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    private val preview = Preview.Builder().build()

    // Deliberately lower resolution than the capture pipeline: ML inference
    // only needs enough detail to find subjects/lighting, not full quality,
    // and keeping it small is what keeps analysis from competing with the
    // preview's frame rate.
    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetResolution(Size(640, 480))
        .build()

    private var lensFacing = LensFacing.BACK
    private var flashMode = FlashMode.OFF

    // null = no manual pick yet, so buildImageCapture() below requests the
    // sensor's highest available still-capture resolution by default (see
    // its KDoc) - this directly answers the "ảnh nhẹ, mất chi tiết" report:
    // ImageCapture previously had no resolution selector at all, so CameraX
    // was free to pick a smaller-than-max size favoring capture latency.
    private var pinnedResolution: Resolution? = null
    private var imageCapture = buildImageCapture()

    override fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView

        controllerScope.launch {
            val provider = awaitCameraProvider()
            cameraProvider = provider
            preview.setSurfaceProvider(previewView.surfaceProvider)
            // PreviewView.viewPort is null until it has a measured width/height,
            // and rebindUseCases() needs it to keep Preview and ImageAnalysis
            // cropped to the same region (see its KDoc) - wait for the first
            // layout pass instead of binding with a null viewport.
            previewView.doOnLayout { rebindUseCases(provider, lifecycleOwner) }
        }
    }

    override fun focusAt(x: Float, y: Float) {
        val view = previewView ?: return
        val activeCamera = camera ?: return
        val point = view.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        activeCamera.cameraControl.startFocusAndMetering(action)
    }

    override fun setFrameAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalysis.setAnalyzer(analysisExecutor, analyzer)
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
        // The front/back cameras can support different resolution sets, so
        // a resolution manually pinned on one lens isn't guaranteed to
        // still make sense on the other - reset to "highest available"
        // rather than risk silently keeping a mismatched pin.
        pinnedResolution = null
        imageCapture = buildImageCapture()
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        controllerScope.launch { rebindUseCases(provider, owner) }
    }

    override fun setFlashMode(flashMode: FlashMode) {
        this.flashMode = flashMode
        imageCapture.flashMode = mapFlashMode(flashMode)
        camera?.cameraControl?.enableTorch(flashMode == FlashMode.TORCH)
        _cameraState.value = _cameraState.value.copy(flashMode = flashMode)
    }

    /**
     * Rebuilds [imageCapture] pinned to [resolution] and rebinds - CameraX's
     * [ResolutionSelector] is fixed at [ImageCapture.Builder] time, it can't
     * be changed on a live use case, so switching resolution means a fresh
     * [ImageCapture] instance and a full rebind (same pattern
     * [setLensFacing] already uses). A [resolution] not present in the
     * current [CameraState.availableResolutions] is silently ignored rather
     * than guessed at - the UI is only ever supposed to offer values from
     * that list.
     */
    override fun setResolution(resolution: Resolution) {
        if (resolution !in _cameraState.value.availableResolutions) return
        pinnedResolution = resolution
        imageCapture = buildImageCapture()
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        controllerScope.launch { rebindUseCases(provider, owner) }
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

    /**
     * Saves through [MediaStore] (public Pictures/FrameWise) instead of
     * `getExternalFilesDir` (app-private storage), which the CameraX
     * `ImageCapture.OutputFileOptions.Builder(File)` overload used
     * previously wrote to - a real photo was being written every time, it
     * just never showed up in the device's Gallery/Photos app since that
     * app-private directory isn't scanned into MediaStore. `RELATIVE_PATH`
     * only exists from API 29 (Q) onward; on API 26-28 the write instead
     * needs `WRITE_EXTERNAL_STORAGE` (declared with `maxSdkVersion="28"`
     * in this module's manifest, requested at runtime alongside CAMERA -
     * see `CameraPermissionState`), and the file lands in the default
     * Pictures root rather than a FrameWise subfolder.
     */
    override suspend fun capturePhoto(): CameraCaptureResult {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "FrameWise_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FrameWise")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        ).build()

        return suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        if (savedUri == null) {
                            continuation.resume(CameraCaptureResult.Failure("Không lấy được đường dẫn ảnh đã lưu"))
                            return
                        }
                        continuation.resume(
                            CameraCaptureResult.Success(
                                CapturedPhoto(
                                    uri = savedUri.toString(),
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

    /**
     * Without a shared [ViewPort], [preview] and [imageAnalysis] each pick
     * their own crop of the sensor - [preview] typically ends up close to
     * the screen's aspect ratio while [imageAnalysis] is pinned to 4:3 (see
     * its `setTargetResolution(640, 480)` above), so on a screen far from
     * 4:3 (nearly every modern phone), the two use cases would see visibly
     * different crops of the same scene. That mismatch is exactly what
     * makes overlays computed from analysis-frame-normalized coordinates
     * (e.g. `BoundingBoxOverlay`, `GridOverlay`) drift from the subject's
     * real on-screen position as the device's aspect ratio departs further
     * from 4:3. Binding both use cases through one [UseCaseGroup] with
     * [PreviewView.getViewPort] forces CameraX to crop them identically, so
     * analysis-frame coordinates map correctly onto the displayed preview
     * regardless of screen aspect ratio.
     */
    private fun rebindUseCases(provider: ProcessCameraProvider, owner: LifecycleOwner) {
        val cameraSelector = when (lensFacing) {
            LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        provider.unbindAll()
        val viewPort = previewView?.viewPort
        val boundCamera = if (viewPort != null) {
            val useCaseGroup = UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .addUseCase(imageAnalysis)
                .build()
            provider.bindToLifecycle(owner, cameraSelector, useCaseGroup)
        } else {
            // Shouldn't happen once bind() only calls this from doOnLayout,
            // but fails safe to the old (potentially misaligned) behavior
            // rather than crashing if some caller invokes this earlier.
            provider.bindToLifecycle(owner, cameraSelector, preview, imageCapture, imageAnalysis)
        }
        camera = boundCamera

        val cameraInfo = boundCamera.cameraInfo
        val zoomState = cameraInfo.zoomState.value
        val exposureState = cameraInfo.exposureState
        val availableResolutions = enumerateJpegResolutions(cameraInfo)

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
            availableResolutions = availableResolutions,
            // pinnedResolution is what we actually asked ImageCapture for;
            // with no pin, buildImageCapture() requested
            // ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY, so the largest
            // entry in the list we just enumerated IS what's in effect -
            // more robust to report here than reading back
            // imageCapture.resolutionInfo, which can lag briefly right
            // after a bind.
            selectedResolution = pinnedResolution ?: availableResolutions.maxByOrNull {
                it.width.toLong() * it.height
            },
        )
    }

    private fun buildImageCapture(): ImageCapture {
        val strategy = pinnedResolution?.let { resolution ->
            ResolutionStrategy(
                Size(resolution.width, resolution.height),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
            )
        } ?: ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY

        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(strategy).build())
            .build()
            .also { it.flashMode = mapFlashMode(flashMode) }
    }

    private fun mapFlashMode(flashMode: FlashMode): Int = when (flashMode) {
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        FlashMode.OFF, FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
    }

    /**
     * Reads the sizes the camera's hardware actually reports for JPEG still
     * capture (via Camera2 interop's [CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP])
     * rather than a hardcoded list - resolution support varies a lot across
     * real devices, so this can only be known at runtime per-device.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun enumerateJpegResolutions(cameraInfo: CameraInfo): List<Resolution> {
        val characteristics = Camera2CameraInfo.from(cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return emptyList()
        val sizes = characteristics.getOutputSizes(ImageFormat.JPEG) ?: return emptyList()

        return sizes
            .map { Resolution(it.width, it.height) }
            .distinct()
            .sortedByDescending { it.width.toLong() * it.height }
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
