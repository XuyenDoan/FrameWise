package com.framewise.domain.model

enum class HorizonLevel {
    LEVEL,
    TILTED_LEFT,
    TILTED_RIGHT,
}

data class HorizonState(
    val angleDegrees: Float = 0f,
    val level: HorizonLevel = HorizonLevel.LEVEL,
) {
    companion object {
        /** Below this, the tilt is imperceptible in a photo — shown as LEVEL. */
        const val LEVEL_THRESHOLD_DEGREES = 0.5f

        fun fromAngle(angleDegrees: Float): HorizonState {
            val level = when {
                angleDegrees > LEVEL_THRESHOLD_DEGREES -> HorizonLevel.TILTED_RIGHT
                angleDegrees < -LEVEL_THRESHOLD_DEGREES -> HorizonLevel.TILTED_LEFT
                else -> HorizonLevel.LEVEL
            }
            return HorizonState(angleDegrees, level)
        }
    }
}
