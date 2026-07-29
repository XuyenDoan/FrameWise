package com.framewise.data.vision

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.NormalizedRect
import com.framewise.domain.model.SubjectLabel
import com.framewise.domain.model.VisionResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
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
 * apart.
 */
internal class MlKitFrameAnalyzer(
    private val onResult: (VisionResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build(),
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build(),
    )

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

        faceDetector.process(inputImage)
            .addOnCompleteListener { faceTask ->
                val faces = if (faceTask.isSuccessful) faceTask.result else emptyList()

                objectDetector.process(inputImage)
                    .addOnCompleteListener { objectTask ->
                        val objects = if (objectTask.isSuccessful) objectTask.result else emptyList()

                        val subjects = buildSubjects(faces, objects, analysisWidth, analysisHeight)
                        val primary = subjects.maxByOrNull { it.boundingBox.area }
                        val lighting = LuminanceEvaluator.evaluate(imageProxy, primary?.boundingBox)

                        onResult(VisionResult(subjects = subjects, lighting = lighting))
                        imageProxy.close()
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
}
