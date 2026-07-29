package com.framewise.domain.usecase

import com.framewise.domain.model.BackgroundState
import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.GuidanceType
import com.framewise.domain.model.HorizonState
import com.framewise.domain.model.LightingState
import com.framewise.domain.model.NormalizedRect
import com.framewise.domain.model.ShootingMode
import com.framewise.domain.model.SubjectLabel
import com.framewise.domain.model.VisionResult
import com.framewise.domain.model.trackingKey
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
    fun `busy background with a subject present suggests changing angle`() {
        val subject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1833f, right = 0.4833f, bottom = 0.4833f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(subject), backgroundState = BackgroundState.BUSY),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).contains(GuidanceType.BUSY_BACKGROUND)
        assertThat(result.score).isEqualTo(90)
    }

    @Test
    fun `busy background is ignored when there is no primary subject`() {
        val result = useCase(
            vision = VisionResult(backgroundState = BackgroundState.BUSY),
            horizon = HorizonState.fromAngle(0f),
        )

        assertThat(result.messages).doesNotContain(GuidanceType.BUSY_BACKGROUND)
    }

    @Test
    fun `landscape mode ignores busy background even with a subject present`() {
        val subject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1833f, right = 0.4833f, bottom = 0.4833f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(subject), backgroundState = BackgroundState.BUSY),
            horizon = HorizonState.fromAngle(0f),
            mode = ShootingMode.LANDSCAPE,
        )

        assertThat(result.messages).doesNotContain(GuidanceType.BUSY_BACKGROUND)
    }

    @Test
    fun `portrait mode auto-picks the face over a larger non-face subject`() {
        // centerX = centerY = 1/3 (on a thirds point) and top = 0.1 (within
        // the 5%-15% ideal headroom band FACE subjects are checked against)
        // so this box alone scores 100 - unlike the reused-PERSON-box
        // pattern elsewhere in this file, a FACE box also has to satisfy
        // the headroom check, not just thirds placement.
        val face = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1f, right = 0.4833f, bottom = 0.5667f),
            label = SubjectLabel.FACE,
            confidence = 0.95f,
        )
        val largerObject = DetectedSubject(
            boundingBox = NormalizedRect(left = 0f, top = 0f, right = 0.9f, bottom = 0.9f),
            label = SubjectLabel.OBJECT,
            confidence = 0.8f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(largerObject, face)),
            horizon = HorizonState.fromAngle(0f),
            mode = ShootingMode.PORTRAIT,
        )

        // The larger OBJECT would trigger MOVE_CLOSER/FARTHER framing logic
        // if it were picked as primary instead of the smaller FACE; with
        // PORTRAIT mode preferring FACE, the face's centered box (already
        // on a thirds point, correct headroom) should score cleanly.
        assertThat(result.score).isEqualTo(100)
    }

    @Test
    fun `animal mode auto-picks the non-face subject over a face`() {
        val face = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.4f, top = 0.4f, right = 0.6f, bottom = 0.6f),
            label = SubjectLabel.FACE,
            confidence = 0.95f,
        )
        val animal = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1833f, right = 0.4833f, bottom = 0.4833f),
            label = SubjectLabel.OBJECT,
            confidence = 0.8f,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(face, animal)),
            horizon = HorizonState.fromAngle(0f),
            mode = ShootingMode.ANIMAL,
        )

        assertThat(result.score).isEqualTo(100)
    }

    @Test
    fun `a tapped subject key overrides the auto-picked primary subject`() {
        val bigButUnselected = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.4f, top = 0.4f, right = 0.6f, bottom = 0.6f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
            trackingId = 1,
        )
        val smallButSelected = DetectedSubject(
            boundingBox = NormalizedRect(left = 0.1833f, top = 0.1833f, right = 0.4833f, bottom = 0.4833f),
            label = SubjectLabel.PERSON,
            confidence = 0.9f,
            trackingId = 2,
        )
        val result = useCase(
            vision = VisionResult(subjects = listOf(bigButUnselected, smallButSelected)),
            horizon = HorizonState.fromAngle(0f),
            selectedSubjectKey = smallButSelected.trackingKey(),
        )

        assertThat(result.score).isEqualTo(100)
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
