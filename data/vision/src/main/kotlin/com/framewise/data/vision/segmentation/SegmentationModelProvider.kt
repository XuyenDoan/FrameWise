package com.framewise.data.vision.segmentation

import android.content.Context
import com.framewise.data.vision.ModelFileDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** See `PoseModelProvider` — identical download-once-cache pattern, different model. */
@Singleton
class SegmentationModelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getModelFile(): File? =
        ModelFileDownloader.download(context, MODEL_FILE_NAME, MODEL_DOWNLOAD_URL, TAG)

    private companion object {
        const val TAG = "SegmentationModelProvider"
        const val MODEL_FILE_NAME = "selfie_segmenter.tflite"
        const val MODEL_DOWNLOAD_URL =
            "https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite"
    }
}
