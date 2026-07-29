package com.framewise.data.vision.segmentation

import android.content.Context
import android.media.Image
import android.util.Log
import com.framewise.domain.model.BackgroundState
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the MediaPipe [ImageSegmenter] (selfie segmentation model) used to
 * estimate how visually "busy" the background behind the primary subject
 * is. Uses [RunningMode.VIDEO] for the same reason
 * `com.framewise.data.vision.pose.PoseFrameProcessor` does — fits the
 * existing synchronous, throttled, single-executor frame analysis chain
 * without a second async callback path to coordinate.
 *
 * [BackgroundClutterScorer] only ever looks at the category mask's own
 * shape — it deliberately does **not** try to combine this with the raw
 * luma-plane texture analysis used elsewhere in this module
 * ([HorizonLineDetector][com.framewise.data.vision.HorizonLineDetector],
 * [LuminanceEvaluator][com.framewise.data.vision.LuminanceEvaluator]),
 * because those scan the sensor-orientation buffer while MediaPipe's mask
 * is in its own (rotation-corrected) coordinate space — pixel-for-pixel
 * combining the two would just be a second instance of the same
 * rotation-mismatch risk already flagged for `HorizonLineDetector`.
 */
@Singleton
class BackgroundSegmentationProcessor @Inject constructor(
    private val modelProvider: SegmentationModelProvider,
    @ApplicationContext private val context: Context,
) {
    private var segmenter: ImageSegmenter? = null
    private var initializationFailed = false

    val isReady: Boolean get() = segmenter != null

    suspend fun ensureInitialized() {
        if (segmenter != null || initializationFailed) return

        val modelFile = modelProvider.getModelFile()
        if (modelFile == null) {
            initializationFailed = true
            return
        }

        runCatching { createSegmenter(modelFile) }
            .onSuccess { segmenter = it }
            .onFailure { error ->
                initializationFailed = true
                Log.w(TAG, "MediaPipe ImageSegmenter init failed - background analysis disabled this session", error)
            }
    }

    /** Must only be called after [ensureInitialized] set [isReady] true. */
    fun detect(mediaImage: Image, rotationDegrees: Int, timestampMs: Long): BackgroundState {
        val activeSegmenter = segmenter ?: return BackgroundState.UNKNOWN
        val mpImage = MediaImageBuilder(mediaImage).build()
        val processingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()

        val result = runCatching {
            activeSegmenter.segmentForVideo(mpImage, processingOptions, timestampMs)
        }.getOrNull() ?: return BackgroundState.UNKNOWN

        val categoryMaskImage = result.categoryMask().orElse(null) ?: return BackgroundState.UNKNOWN
        val maskBuffer = runCatching { ByteBufferExtractor.extract(categoryMaskImage) }.getOrNull()
            ?: return BackgroundState.UNKNOWN

        return BackgroundClutterScorer.score(maskBuffer, categoryMaskImage.width, categoryMaskImage.height)
    }

    private fun createSegmenter(modelFile: File): ImageSegmenter {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelFile.absolutePath)
            .build()

        val options = ImageSegmenter.ImageSegmenterOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setOutputCategoryMask(true)
            .setOutputConfidenceMasks(false)
            .build()

        return ImageSegmenter.createFromOptions(context, options)
    }

    private companion object {
        const val TAG = "BackgroundSegmentationProcessor"
    }
}
