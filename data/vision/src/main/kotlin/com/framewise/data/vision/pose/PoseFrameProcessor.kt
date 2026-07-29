package com.framewise.data.vision.pose

import android.content.Context
import android.media.Image
import android.util.Log
import com.framewise.domain.model.PoseLandmark
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the MediaPipe [PoseLandmarker] instance. Uses [RunningMode.VIDEO]
 * (synchronous `detectForVideo`) rather than `LIVE_STREAM` deliberately:
 * we already run on our own throttled, single-thread analysis executor
 * (see `CameraXController`'s `analysisExecutor`), so a blocking call here
 * fits the existing sequential face->object->pose chain in
 * `MlKitFrameAnalyzer` without needing a second async callback path to
 * coordinate against the shared `ImageProxy`'s lifecycle.
 */
@Singleton
class PoseFrameProcessor @Inject constructor(
    private val poseModelProvider: PoseModelProvider,
    @ApplicationContext private val context: Context,
) {
    private var poseLandmarker: PoseLandmarker? = null
    private var initializationFailed = false

    val isReady: Boolean get() = poseLandmarker != null

    suspend fun ensureInitialized() {
        if (poseLandmarker != null || initializationFailed) return

        val modelFile = poseModelProvider.getModelFile()
        if (modelFile == null) {
            initializationFailed = true
            return
        }

        runCatching { createLandmarker(modelFile) }
            .onSuccess { poseLandmarker = it }
            .onFailure { error ->
                initializationFailed = true
                Log.w(TAG, "MediaPipe PoseLandmarker init failed - Pose Assistant disabled this session", error)
            }
    }

    /** Must only be called after [ensureInitialized] set [isReady] true. */
    fun detect(mediaImage: Image, rotationDegrees: Int, timestampMs: Long): List<PoseLandmark> {
        val landmarker = poseLandmarker ?: return emptyList()
        val mpImage = MediaImageBuilder(mediaImage).build()
        val processingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()

        val result: PoseLandmarkerResult = runCatching {
            landmarker.detectForVideo(mpImage, processingOptions, timestampMs)
        }.getOrNull() ?: return emptyList()

        val landmarks = result.landmarks().firstOrNull() ?: return emptyList()
        return landmarks.map { landmark ->
            PoseLandmark(
                x = landmark.x(),
                y = landmark.y(),
                z = landmark.z(),
                visibility = if (landmark.visibility().isPresent) landmark.visibility().get() else 1f,
            )
        }
    }

    private fun createLandmarker(modelFile: File): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelFile.absolutePath)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .build()

        return PoseLandmarker.createFromOptions(context, options)
    }

    private companion object {
        const val TAG = "PoseFrameProcessor"
    }
}
