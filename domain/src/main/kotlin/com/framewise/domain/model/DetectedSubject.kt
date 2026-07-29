package com.framewise.domain.model

/**
 * NOTE: ML Kit's on-device (unbundled) Object Detection API only classifies
 * 5 coarse categories (fashion goods, food, home goods, place, plants) — it
 * cannot tell a dog from a cat or a car from a bicycle. [DOG], [CAT],
 * [VEHICLE] and [BICYCLE] are kept here for the domain model's sake (and
 * for a future custom TFLite/MediaPipe classifier swap-in per the Phase 1
 * roadmap), but `data:vision`'s current implementation only ever emits
 * [FACE] (from ML Kit Face Detection), [FOOD] (from the one ML Kit object
 * category that maps cleanly), and [OBJECT] (everything else it detects a
 * bounding box for, uncategorized).
 */
enum class SubjectLabel {
    PERSON,
    FACE,
    DOG,
    CAT,
    VEHICLE,
    BICYCLE,
    FOOD,
    OBJECT,
}

data class DetectedSubject(
    val boundingBox: NormalizedRect,
    val label: SubjectLabel,
    val confidence: Float,
)

enum class LightingState {
    GOOD,
    UNDEREXPOSED,
    OVEREXPOSED,
    BACKLIT,
}

data class VisionResult(
    val subjects: List<DetectedSubject> = emptyList(),
    val lighting: LightingState = LightingState.GOOD,
    val scene: SceneType = SceneType.UNKNOWN,
)
