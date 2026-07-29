package com.framewise.domain.repository

import com.framewise.domain.model.HorizonState
import kotlinx.coroutines.flow.StateFlow

interface SensorRepository {
    val horizonState: StateFlow<HorizonState>
}
