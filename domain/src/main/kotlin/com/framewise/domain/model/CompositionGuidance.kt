package com.framewise.domain.model

/**
 * Ranked so the UI shows at most the top 1-2 items instead of listing every
 * imperfection at once (see Phase 1 architecture doc, section 6).
 */
enum class GuidanceType(val priority: Int) {
    LEVEL_HORIZON(0),
    MOVE_LEFT(1),
    MOVE_RIGHT(1),
    RAISE_CAMERA(1),
    LOWER_CAMERA(1),
    MOVE_CLOSER(2),
    MOVE_FARTHER(2),
    IMPROVE_LIGHTING(3),
    GOOD(99),
}

data class CompositionGuidance(
    val messages: List<GuidanceType> = listOf(GuidanceType.GOOD),
    val score: Int = 100,
)
