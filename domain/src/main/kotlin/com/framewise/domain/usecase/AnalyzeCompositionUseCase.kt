package com.framewise.domain.usecase

import com.framewise.domain.model.BackgroundState
import com.framewise.domain.model.CompositionGuidance
import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.GuidanceType
import com.framewise.domain.model.HorizonLevel
import com.framewise.domain.model.HorizonState
import com.framewise.domain.model.LightingState
import com.framewise.domain.model.ShootingMode
import com.framewise.domain.model.SubjectLabel
import com.framewise.domain.model.VisionResult
import com.framewise.domain.model.trackingKey
import javax.inject.Inject
import kotlin.math.abs

/**
 * Pure function: (vision, horizon, mode, selectedSubjectKey) -> guidance +
 * score. Deliberately holds no state and touches no Android API, so it's
 * testable with plain inputs (see AnalyzeCompositionUseCaseTest) without
 * faking CameraX or ML Kit.
 */
class AnalyzeCompositionUseCase @Inject constructor() {

    operator fun invoke(
        vision: VisionResult,
        horizon: HorizonState,
        mode: ShootingMode = ShootingMode.AUTO,
        selectedSubjectKey: String? = null,
    ): CompositionGuidance {
        var score = 100
        val messages = mutableListOf<GuidanceType>()

        val primarySubject = pickPrimarySubject(vision.subjects, mode, selectedSubjectKey)

        if (primarySubject != null) {
            val box = primarySubject.boundingBox
            val target = nearestThirdsPoint(box.centerX, box.centerY)
            val dx = box.centerX - target.first

            // Subject sits right of the target thirds point -> aim the
            // camera further right so the optical axis swings toward it,
            // pulling the subject back in toward that target point.
            if (abs(dx) > POSITION_THRESHOLD) {
                messages += if (dx > 0) GuidanceType.MOVE_RIGHT else GuidanceType.MOVE_LEFT
                score -= 15
            }

            if (primarySubject.label == SubjectLabel.FACE) {
                // Portrait-specific: headroom (space above the head) matters
                // more than generic thirds placement for the vertical axis.
                val headroom = box.top
                when {
                    headroom < MIN_HEADROOM -> {
                        messages += GuidanceType.RAISE_CAMERA
                        score -= 15
                    }
                    headroom > MAX_HEADROOM -> {
                        messages += GuidanceType.LOWER_CAMERA
                        score -= 15
                    }
                }
            } else {
                val dy = box.centerY - target.second
                if (abs(dy) > POSITION_THRESHOLD) {
                    messages += if (dy > 0) GuidanceType.LOWER_CAMERA else GuidanceType.RAISE_CAMERA
                    score -= 15
                }
            }

            when {
                box.area < MIN_SUBJECT_AREA -> {
                    messages += GuidanceType.MOVE_CLOSER
                    score -= 10
                }
                box.area > MAX_SUBJECT_AREA -> {
                    messages += GuidanceType.MOVE_FARTHER
                    score -= 10
                }
            }

            // Isolating a subject from a cluttered background is a
            // portrait/animal concern, not a landscape one - a landscape
            // shot usually *wants* the whole busy scene in frame.
            if (mode != ShootingMode.LANDSCAPE && vision.backgroundState == BackgroundState.BUSY) {
                messages += GuidanceType.BUSY_BACKGROUND
                score -= 10
            }
        } else {
            // No dominant foreground subject - typical of a landscape shot.
            // Place the detected horizon *line* (computer-vision estimate
            // of where sky meets ground, not the sensor's roll angle) at
            // the nearest thirds line instead of leaving it wherever it
            // happens to fall.
            vision.horizonLineY?.let { horizonY ->
                val target = nearestHorizonThird(horizonY)
                val dy = horizonY - target
                if (abs(dy) > POSITION_THRESHOLD) {
                    messages += if (dy > 0) GuidanceType.LOWER_CAMERA else GuidanceType.RAISE_CAMERA
                    score -= 10
                }
            }
        }

        when (horizon.level) {
            HorizonLevel.LEVEL -> Unit
            else -> {
                messages += GuidanceType.LEVEL_HORIZON
                score -= if (abs(horizon.angleDegrees) > SEVERE_TILT_DEGREES) 20 else 10
            }
        }

        if (vision.lighting != LightingState.GOOD) {
            messages += GuidanceType.IMPROVE_LIGHTING
            score -= 10
        }

        val rankedMessages = if (messages.isEmpty()) {
            listOf(GuidanceType.GOOD)
        } else {
            messages.sortedBy { it.priority }
        }

        return CompositionGuidance(
            messages = rankedMessages,
            score = score.coerceIn(0, 100),
        )
    }

    /**
     * A user's tap always wins when it still matches something in the
     * current frame (see [DetectedSubject.trackingId]'s KDoc for when it
     * stops matching). Otherwise falls back to an auto pick that leans on
     * [mode]: PORTRAIT prefers a FACE if one is present (a person
     * incidentally in an animal shot, or vice-versa, shouldn't hijack the
     * primary subject), ANIMAL prefers a non-FACE subject (ML Kit can't
     * name species - see [SubjectLabel]'s KDoc - so "the animal" is
     * whatever isn't a human face), and AUTO/LANDSCAPE make no such
     * preference. Any preference that finds nothing just falls through to
     * "largest box wins", same as before mode existed.
     */
    private fun pickPrimarySubject(
        subjects: List<DetectedSubject>,
        mode: ShootingMode,
        selectedSubjectKey: String?,
    ): DetectedSubject? {
        if (selectedSubjectKey != null) {
            subjects.find { it.trackingKey() == selectedSubjectKey }?.let { return it }
        }

        val preferred = when (mode) {
            ShootingMode.PORTRAIT -> subjects.filter { it.label == SubjectLabel.FACE }
            ShootingMode.ANIMAL -> subjects.filter { it.label != SubjectLabel.FACE }
            ShootingMode.AUTO, ShootingMode.LANDSCAPE -> subjects
        }
        return preferred.ifEmpty { subjects }.maxByOrNull { it.boundingBox.area }
    }

    /** Nearest of the 4 rule-of-thirds intersection points to (x, y). */
    private fun nearestThirdsPoint(x: Float, y: Float): Pair<Float, Float> {
        val xs = floatArrayOf(1f / 3f, 2f / 3f)
        val ys = floatArrayOf(1f / 3f, 2f / 3f)
        var best = xs[0] to ys[0]
        var bestDistance = Float.MAX_VALUE
        for (candidateX in xs) {
            for (candidateY in ys) {
                val distance = (candidateX - x) * (candidateX - x) + (candidateY - y) * (candidateY - y)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = candidateX to candidateY
                }
            }
        }
        return best
    }

    /** Nearest of the 2 horizontal thirds lines (top or bottom) to y. */
    private fun nearestHorizonThird(y: Float): Float {
        val ys = floatArrayOf(1f / 3f, 2f / 3f)
        return ys.minByOrNull { abs(it - y) } ?: ys[0]
    }

    private companion object {
        const val POSITION_THRESHOLD = 0.08f
        const val MIN_SUBJECT_AREA = 0.05f
        const val MAX_SUBJECT_AREA = 0.6f
        const val SEVERE_TILT_DEGREES = 5f

        /** Ideal headroom for a portrait: 5%-15% of frame height above the head. */
        const val MIN_HEADROOM = 0.05f
        const val MAX_HEADROOM = 0.15f
    }
}
