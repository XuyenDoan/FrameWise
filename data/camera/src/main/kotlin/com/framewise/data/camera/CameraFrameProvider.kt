package com.framewise.data.camera

import androidx.camera.core.ImageAnalysis

/**
 * Lets `data:vision` register itself as the analyzer for the single
 * `ImageAnalysis` use case CameraX allows per session, without `data:vision`
 * needing to own (or duplicate) the `ProcessCameraProvider`/lifecycle
 * binding that [CameraXController] already manages.
 */
interface CameraFrameProvider {
    fun setFrameAnalyzer(analyzer: ImageAnalysis.Analyzer)
}
