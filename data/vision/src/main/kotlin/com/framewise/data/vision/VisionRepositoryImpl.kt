package com.framewise.data.vision

import com.framewise.data.camera.CameraFrameProvider
import com.framewise.data.vision.pose.PoseFrameProcessor
import com.framewise.domain.model.PoseLandmark
import com.framewise.domain.model.VisionResult
import com.framewise.domain.repository.PoseRepository
import com.framewise.domain.repository.VisionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Registers [MlKitFrameAnalyzer] as the camera's frame analyzer once, at
 * construction time — since both this and [CameraFrameProvider]'s
 * implementation are Hilt singletons, this wiring only happens once for the
 * whole app, and nothing else needs to remember to connect them.
 *
 * Also owns [PoseRepository]: pose detection shares the same single
 * [ImageAnalysis][androidx.camera.core.ImageAnalysis] frame stream as
 * face/object/scene detection (CameraX only allows one analyzer), so it
 * has to be driven from the same [MlKitFrameAnalyzer] instance rather than
 * a separate repository registering its own analyzer.
 */
@Singleton
class VisionRepositoryImpl @Inject constructor(
    frameProvider: CameraFrameProvider,
    poseFrameProcessor: PoseFrameProcessor,
) : VisionRepository, PoseRepository {

    private val _visionResults = MutableStateFlow(VisionResult())
    override val visionResults: StateFlow<VisionResult> = _visionResults.asStateFlow()

    private val _poseLandmarks = MutableStateFlow<List<PoseLandmark>>(emptyList())
    override val poseLandmarks: StateFlow<List<PoseLandmark>> = _poseLandmarks.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch { poseFrameProcessor.ensureInitialized() }

        frameProvider.setFrameAnalyzer(
            MlKitFrameAnalyzer(
                poseFrameProcessor = poseFrameProcessor,
                onResult = { result -> _visionResults.value = result },
                onPoseResult = { landmarks -> _poseLandmarks.value = landmarks },
            ),
        )
    }
}
