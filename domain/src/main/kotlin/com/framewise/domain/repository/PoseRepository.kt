package com.framewise.domain.repository

import com.framewise.domain.model.PoseLandmark
import kotlinx.coroutines.flow.StateFlow

interface PoseRepository {
    /** Empty when no person is detected, or the pose model isn't ready yet. */
    val poseLandmarks: StateFlow<List<PoseLandmark>>
}
