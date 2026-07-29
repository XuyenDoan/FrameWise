package com.framewise.domain.usecase

import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.GuidanceType
import com.framewise.domain.model.HorizonState
import com.framewise.domain.model.LightingState
import com.framewise.domain.model.NormalizedRect
import com.framewise.domain.model.SubjectLabel
import com.framewise.domain.model.VisionResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalyzeCompositionUseCaseTest {

    private val useCase = AnalyzeCompositionUseCase()

    @Test
    fun `subject centered on a thirds point with level horizon scores 100`() {
        val subject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1833f, right = 0.4833f, bottom = 0.4833f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(subject), lighting = LightingState.GOOD),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.score).isEqualTo(100)
        assertThat(result.messages).containsExactly(GuidanceType.GOOD)
    }

    @Test
    fun `subject dead-center suggests moving right and lowering camera`() {
        val subject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.4f, top = 0.4f, right = 0.6f, bottom = 0.6f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(subject)),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.MOVE_RIGHT)
        assertThat(result.messages).contains(GuidanceType.LOWER_CAMERA)
        assertThat(result.score).isLessThan(100)
    }

    @Test
    fun `tiny subject suggests moving closer`() {
        val subject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.32f, top = 0.32f, right = 0.35f, bottom = 0.35f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(subject)),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.MOVE_CLOSER)
    }

    @Test
    fun `face with too little headroom suggests raising camera`() {
        val face = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.3f, top = 0.01f, right = 0.6f, bottom = 0.3f),
            label = SubjectLabel.FACE,
            confidence = 0.95f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(face)),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.RAISE_CAMERA)
    }

    @Test
    fun `face with too much headroom suggests lowering camera`() {
        val face = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.3f, top = 0.3f, right = 0.6f, bottom = 0.55f),
            label = SubjectLabel.FACE,
            confidence = 0.95f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(face)),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.LOWER_CAMERA)
    }

    @Test
    fun `tilted horizon suggests leveling and lowers score`() {
        val result = useCase(
            vision = VisionResult(),
            horizon = HorizonState.fromAngle(8f),
        )

        assertThat(result.messages).contains(GuidanceType.LEVEL_HORIZON)
        assertThat(result.score).isEqualTo(80)
    }

    @Test
    fun `horizon line dead center with no subject suggests lowering camera`() {
        val result = useCase(
            vision = VisionResult(horizonLineY = 0.5f),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.LOWER_CAMERA)
    }

    @Test
    fun `horizon line already near a third with no subject scores 100`() {
        val result = useCase(
            vision = VisionResult(horizonLineY = 1f / 3f),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.score).isEqualTo(100)
        assertThat(result.messages).containsExactly(GuidanceType.GOOD)
    }

    @Test
    fun `horizon line placement is ignored when a primary subject is present`() {
        val subject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1833f, right = 0.4833f, bottom = 0.4833f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(subject), horizonLineY = 0.5f),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.score).isEqualTo(100)
        assertThat(result.messages).containsExactly(GuidanceType.GOOD)
    }

    @Test
    fun `bad lighting suggests improving lighting`() {
        val result = useCase(
            vision = VisionResult(lighting = LightingState.BACKLIT),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.IMPROVE_LIGHTING)
        assertThat(result.score).isEqualTo(90)
    }
}
