package com.framewise.data.vision

import android.content.Context
import android.util.Log
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared download-once-cache-forever logic behind both `PoseModelProvider`
 * and `SegmentationModelProvider`: MediaPipe Tasks models have no
 * in-app-binary option (too large to vendor as repo assets), so each is
 * fetched to app-private storage the first time it's needed.
 *
 * Never throws — a failed download (offline, blocked, URL moved) just
 * means the caller gets `null` and the optional feature stays disabled
 * for that session, exactly like every other best-effort heuristic in
 * this module.
 */
internal object ModelFileDownloader {

    suspend fun download(context: Context, fileName: String, url: String, tag: String): File? =
        withContext(Dispatchers.IO) {
            val existing = File(context.filesDir, fileName)
            if (existing.exists() && existing.length() > 0) return@withContext existing

            runCatching {
                val tempFile = File(context.cacheDir, "$fileName.download")
                URL(url).openStream().use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempFile.copyTo(existing, overwrite = true)
                tempFile.delete()
                existing
            }.onFailure { error ->
                Log.w(tag, "Model download failed for $fileName - feature disabled this session", error)
            }.getOrNull()
        }
}
