package com.framewise.domain.repository

import com.framewise.domain.model.VisionResult
import kotlinx.coroutines.flow.StateFlow

interface VisionRepository {
    val visionResults: StateFlow<VisionResult>
}
