package com.framewise.data.vision.pose

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaPipe Tasks needs a local `.task` model file — unlike ML Kit's
 * bundled detectors, there is no in-app-binary option, and the model
 * (~5-9 MB for the "lite" pose variant) is too large to vendor as a repo
 * asset here. This downloads it once, on first use, to app-private
 * storage, and reuses the cached file afterwards.
 *
 * Requires network access the *first* time the camera screen runs Pose
 * Assistant; if that fails (offline, blocked, URL moved), [getModelFile]
 * returns null and pose guidance is simply unavailable for that session —
 * this must never crash or block the rest of the camera experience over
 * an optional enhancement.
 */
@Singleton
class PoseModelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val modelFile: File
        get() = File(context.filesDir, MODEL_FILE_NAME)

    suspend fun getModelFile(): File? = withContext(Dispatchers.IO) {
        val existing = modelFile
        if (existing.exists() && existing.length() > 0) return@withContext existing

        runCatching {
            val tempFile = File(context.cacheDir, "$MODEL_FILE_NAME.download")
            URL(MODEL_DOWNLOAD_URL).openStream().use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile.copyTo(existing, overwrite = true)
            tempFile.delete()
            existing
        }.onFailure { error ->
            Log.w(TAG, "Pose model download failed - Pose Assistant will stay disabled this session", error)
        }.getOrNull()
    }

    private companion object {
        const val TAG = "PoseModelProvider"
        const val MODEL_FILE_NAME = "pose_landmarker_lite.task"
        const val MODEL_DOWNLOAD_URL =
            "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"
    }
}
