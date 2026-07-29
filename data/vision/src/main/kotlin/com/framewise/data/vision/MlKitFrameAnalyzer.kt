package com.framewise.data.vision

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.framewise.data.vision.pose.PoseFrameProcessor
import com.framewise.data.vision.segmentation.BackgroundSegmentationProcessor
import com.framewise.domain.model.BackgroundState
import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.NormalizedRect
import com.framewise.domain.model.PoseLandmark
import com.framewise.domain.model.SceneType
import com.framewise.domain.model.SubjectLabel
import com.framewise.domain.model.VisionResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * Runs ML Kit Face Detection + Object Detection (in that order, chained
 * through their callbacks rather than in parallel — simpler to reason
 * about correctness-wise than juggling two concurrent Tasks over the same
 * [ImageProxy]) on every analysis frame CameraX delivers, then folds the
 * results plus [LuminanceEvaluator]'s exposure check into one
 * [VisionResult].
 *
 * See [com.framewise.domain.model.SubjectLabel]'s KDoc for why detected
 * objects only ever come back as [SubjectLabel.FOOD] or
 * [SubjectLabel.OBJECT] — ML Kit's unbundled classifier can't tell species
 * apart. Scene classification (via ML Kit Image Labeling + [SceneClassifier])
 * runs separately, throttled to every [SCENE_ANALYSIS_INTERVAL]th frame.
 * [HorizonLineDetector] only runs when no subject was found (see its KDoc
 * for a known coordinate-space limitation).
 */
internal class MlKitFrameAnalyzer(
    private val poseFrameProcessor: PoseFrameProcessor,
    private val backgroundSegmentationProcessor: BackgroundSegmentationProcessor,
    private val onResult: (VisionResult) -> Unit,
    private val onPoseResult: (List<PoseLandmark>) -> Unit,
) : ImageAnalysis.Analyzer {

    // enableTracking() is required for Face.getTrackingId() to return a
    // real id instead of -1 - see DetectedSubject.trackingId's KDoc for why
    // this backs tap-to-select-subject.
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build(),
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build(),
    )

    private val imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    // Scene rarely changes frame-to-frame, unlike subject position - running
    // the labeler on every frame would be wasted work competing with the
    // detectors that actually need to be near-real-time.
    private var frameCount = 0
    private var lastScene = SceneType.UNKNOWN
    private var lastPoseTimestampMs = 0L
    private var lastSegmentationTimestampMs = 0L
    private var lastBackgroundState = BackgroundState.UNKNOWN

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val (analysisWidth, analysisHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.height to imageProxy.width
        } else {
            imageProxy.width to imageProxy.height
        }

        frameCount++
        val shouldClassifyScene = frameCount % SCENE_ANALYSIS_INTERVAL == 0
        val shouldDetectPose = poseFrameProcessor.isReady && frameCount % POSE_ANALYSIS_INTERVAL == 0
        val shouldAnalyzeBackground =
            backgroundSegmentationProcessor.isReady && frameCount % SEGMENTATION_ANALYSIS_INTERVAL == 0

        faceDetector.process(inputImage)
            .addOnCompleteListener { faceTask ->
                val faces = if (faceTask.isSuccessful) faceTask.result else emptyList()

                objectDetector.process(inputImage)
                    .addOnCompleteListener { objectTask ->
                        val objects = if (objectTask.isSuccessful) objectTask.result else emptyList()

                        fun finish() {
                            val subjects = buildSubjects(faces, objects, analysisWidth, analysisHeight)
                            val primary = subjects.maxByOrNull { it.boundingBox.area }
                            val lighting = LuminanceEvaluator.evaluate(imageProxy, primary?.boundingBox)
                            // Only worth the scan when there's no foreground
                            // subject - AnalyzeCompositionUseCase ignores this
                            // value entirely whenever a primary subject exists.
                            val horizonLineY = if (subjects.isEmpty()) HorizonLineDetector.detect(imageProxy) else null

                            if (shouldDetectPose) {
                                // MediaPipe requires strictly increasing timestamps for VIDEO mode.
                                val timestampMs = (imageProxy.imageInfo.timestamp / 1_000_000)
                                    .coerceAtLeast(lastPoseTimestampMs + 1)
                                lastPoseTimestampMs = timestampMs
                                onPoseResult(poseFrameProcessor.detect(mediaImage, rotationDegrees, timestampMs))
                            }

                            // Symmetric to horizon-line: only meaningful when
                            // there IS a subject to separate from a background.
                            // Cached across throttled-out frames (like scene)
                            // so guidance doesn't flicker every few frames.
                            if (subjects.isEmpty()) {
                                lastBackgroundState = BackgroundState.UNKNOWN
                            } else if (shouldAnalyzeBackground) {
                                val timestampMs = (imageProxy.imageInfo.timestamp / 1_000_000)
                                    .coerceAtLeast(lastSegmentationTimestampMs + 1)
                                lastSegmentationTimestampMs = timestampMs
                                lastBackgroundState =
                                    backgroundSegmentationProcessor.detect(mediaImage, rotationDegrees, timestampMs)
                            }

                            onResult(
                                VisionResult(
                                    subjects = subjects,
                                    lighting = lighting,
                                    scene = lastScene,
                                    horizonLineY = horizonLineY,
                                    backgroundState = lastBackgroundState,
                                ),
                            )
                            imageProxy.close()
                        }

                        if (shouldClassifyScene) {
                            imageLabeler.process(inputImage)
                                .addOnCompleteListener { labelTask ->
                                    if (labelTask.isSuccessful) {
                                        lastScene = SceneClassifier.classify(labelTask.result)
                                    }
                                    finish()
                                }
                        } else {
                            finish()
                        }
                    }
            }
    }

    private fun buildSubjects(
        faces: List<Face>,
        objects: List<DetectedObject>,
        width: Int,
        height: Int,
    ): List<DetectedSubject> {
        val faceSubjects = faces.map { face ->
            DetectedSubject(
                boundingBox = face.boundingBox.toNormalizedRect(width, height),
                label = SubjectLabel.FACE,
                confidence = 1f,
                // -1 means tracking wasn't available for this detection even
                // though enableTracking() was requested (documented ML Kit
                // fallback) - normalize that to null like DetectedObject's
                // nullable trackingId below.
                trackingId = face.trackingId.takeIf { it != -1 },
            )
        }

        val objectSubjects = objects.map { detected ->
            val topLabel = detected.labels.maxByOrNull { it.confidence }
            val label = if (topLabel?.text?.equals("Food", ignoreCase = true) == true) {
                SubjectLabel.FOOD
            } else {
                SubjectLabel.OBJECT
            }
            DetectedSubject(
                boundingBox = detected.boundingBox.toNormalizedRect(width, height),
                label = label,
                confidence = topLabel?.confidence ?: 0.5f,
                trackingId = detected.trackingId,
            )
        }

        return faceSubjects + objectSubjects
    }

    private fun Rect.toNormalizedRect(width: Int, height: Int): NormalizedRect = NormalizedRect(
        left = (left.toFloat() / width).coerceIn(0f, 1f),
        top = (top.toFloat() / height).coerceIn(0f, 1f),
        right = (right.toFloat() / width).coerceIn(0f, 1f),
        bottom = (bottom.toFloat() / height).coerceIn(0f, 1f),
    )

    private companion object {
        const val SCENE_ANALYSIS_INTERVAL = 15
        const val POSE_ANALYSIS_INTERVAL = 5
        const val SEGMENTATION_ANALYSIS_INTERVAL = 8
    }
}
