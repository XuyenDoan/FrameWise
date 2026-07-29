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

/**
 * [trackingId] comes straight from ML Kit's own tracker (Face Detection's
 * `enableTracking()`, Object Detection's automatic STREAM_MODE tracking) -
 * not something this app computes itself. It lets the UI let a user tap a
 * subject and have that selection follow it across frames. It's still only
 * as reliable as ML Kit's tracker: null when tracking wasn't available for
 * that detection, and a selection tied to an id that stops appearing (the
 * subject left the frame, tracking was lost, occlusion) simply stops
 * matching anything - callers must fail back to no-selection rather than
 * guess, they must never invent a substitute.
 */
data class DetectedSubject(
    val boundingBox: NormalizedRect,
    val label: SubjectLabel,
    val confidence: Float,
    val trackingId: Int? = null,
)

/** Stable-ish key for tying a user's tap to this subject across frames - see [DetectedSubject.trackingId]. */
fun DetectedSubject.trackingKey(): String? = trackingId?.let { "$label:$it" }

enum class LightingState {
    GOOD,
    UNDEREXPOSED,
    OVEREXPOSED,
    BACKLIT,
}

/**
 * Coarse read on how visually "busy" the area *behind* the primary subject
 * is, from MediaPipe Selfie Segmentation's foreground/background mask +
 * a texture measure over the background region alone (see
 * `BackgroundSegmentationProcessor` in data:vision). [UNKNOWN] covers both
 * "no primary subject to separate a background from" and "segmentation
 * model not ready yet" — [BUSY] is only ever reported when there was
 * enough signal to be reasonably sure, matching the fail-closed pattern
 * used everywhere else in this pipeline ([HorizonLineDetector][com.framewise.data.vision.HorizonLineDetector],
 * [SceneClassifier][com.framewise.data.vision.SceneClassifier]).
 */
enum class BackgroundState {
    CLEAN,
    BUSY,
    UNKNOWN,
}

data class VisionResult(
    val subjects: List<DetectedSubject> = emptyList(),
    val lighting: LightingState = LightingState.GOOD,
    val scene: SceneType = SceneType.UNKNOWN,
    /**
     * Vertical position (0f..1f, top to bottom) of the detected horizon
     * line in the frame — distinct from [HorizonState][com.framewise.domain.model.HorizonState]'s
     * sensor-based roll angle, this is a computer-vision estimate of
     * *where in the image* the horizon sits, needed to check placement
     * against the rule of thirds. Null when no horizon was confidently
     * detected (indoor scenes, no clear sky/ground split, etc.) — callers
     * must not guess a value when this is null.
     */
    val horizonLineY: Float? = null,
    val backgroundState: BackgroundState = BackgroundState.UNKNOWN,
)
