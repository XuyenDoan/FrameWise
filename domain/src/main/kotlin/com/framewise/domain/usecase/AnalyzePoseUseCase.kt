package com.framewise.domain.usecase

import com.framewise.domain.model.PoseLandmark
import com.framewise.domain.model.PoseSuggestion
import javax.inject.Inject
import kotlin.math.abs

/**
 * Pure heuristic over MediaPipe Pose's landmark indices 0 (nose), 11 (left
 * shoulder) and 12 (right shoulder) only — deliberately not a full pose
 * classifier. Two checks:
 *  - shoulder height difference relative to shoulder width -> slouching/
 *    uneven shoulders (RELAX_SHOULDERS).
 *  - vertical nose-to-shoulder-midpoint distance relative to shoulder
 *    width, as a rough proxy for "chin tucked down" (RAISE_CHIN).
 *
 * Both thresholds are hand-picked, not calibrated against real photos —
 * expect false positives/negatives; this is an MVP, not a trained model.
 */
class AnalyzePoseUseCase @Inject constructor() {

    operator fun invoke(landmarks: List<PoseLandmark>): List<PoseSuggestion> {
        if (landmarks.size <= RIGHT_SHOULDER_INDEX) return listOf(PoseSuggestion.NONE)

        val nose = landmarks[NOSE_INDEX]
        val leftShoulder = landmarks[LEFT_SHOULDER_INDEX]
        val rightShoulder = landmarks[RIGHT_SHOULDER_INDEX]

        if (leftShoulder.visibility < MIN_VISIBILITY || rightShoulder.visibility < MIN_VISIBILITY) {
            return listOf(PoseSuggestion.NONE)
        }

        val shoulderWidth = abs(rightShoulder.x - leftShoulder.x).coerceAtLeast(MIN_SHOULDER_WIDTH)
        val suggestions = mutableListOf<PoseSuggestion>()

        val shoulderHeightDiff = abs(leftShoulder.y - rightShoulder.y)
        if (shoulderHeightDiff / shoulderWidth > SHOULDER_TILT_RATIO) {
            suggestions += PoseSuggestion.RELAX_SHOULDERS
        }

        val shoulderMidY = (leftShoulder.y + rightShoulder.y) / 2f
        val neckLength = shoulderMidY - nose.y
        if (neckLength < shoulderWidth * CHIN_DOWN_RATIO) {
            suggestions += PoseSuggestion.RAISE_CHIN
        }

        return suggestions.ifEmpty { listOf(PoseSuggestion.NONE) }
    }

    private companion object {
        const val NOSE_INDEX = 0
        const val LEFT_SHOULDER_INDEX = 11
        const val RIGHT_SHOULDER_INDEX = 12

        const val MIN_VISIBILITY = 0.5f
        const val MIN_SHOULDER_WIDTH = 0.01f
        const val SHOULDER_TILT_RATIO = 0.15f
        const val CHIN_DOWN_RATIO = 0.35f
    }
}
