package com.framewise.data.vision

import com.framewise.data.camera.CameraFrameProvider
import com.framewise.domain.model.VisionResult
import com.framewise.domain.repository.VisionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registers [MlKitFrameAnalyzer] as the camera's frame analyzer once, at
 * construction time — since both this and [CameraFrameProvider]'s
 * implementation are Hilt singletons, this wiring only happens once for the
 * whole app, and nothing else needs to remember to connect them.
 */
@Singleton
class VisionRepositoryImpl @Inject constructor(
    frameProvider: CameraFrameProvider,
) : VisionRepository {

    private val _visionResults = MutableStateFlow(VisionResult())
    override val visionResults: StateFlow<VisionResult> = _visionResults.asStateFlow()

    init {
        frameProvider.setFrameAnalyzer(
            MlKitFrameAnalyzer(onResult = { result -> _visionResults.value = result }),
        )
    }
}
