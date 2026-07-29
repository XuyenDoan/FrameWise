package com.framewise.domain.usecase

import com.framewise.domain.model.PoseLandmark
import com.framewise.domain.model.PoseSuggestion
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalyzePoseUseCaseTest {

    private val useCase = AnalyzePoseUseCase()

    private fun landmarks(nose: PoseLandmark, leftShoulder: PoseLandmark, rightShoulder: PoseLandmark): List<PoseLandmark> {
        val list = MutableList(13) { PoseLandmark(0f, 0f, 0f, 1f) }
        list[0] = nose
        list[11] = leftShoulder
        list[12] = rightShoulder
        return list
    }

    @Test
    fun `level shoulders and upright neck returns NONE`() {
        val result = useCase(
            landmarks(
                nose = PoseLandmark(0.5f, 0.2f, 0f, 1f),
                leftShoulder = PoseLandmark(0.35f, 0.4f, 0f, 1f),
                rightShoulder = PoseLandmark(0.65f, 0.4f, 0f, 1f),
            ),
        )
        assertThat(result).containsExactly(PoseSuggestion.NONE)
    }

    @Test
    fun `uneven shoulders trigger RELAX_SHOULDERS`() {
        val result = useCase(
            landmarks(
                nose = PoseLandmark(0.5f, 0.2f, 0f, 1f),
                leftShoulder = PoseLandmark(0.35f, 0.3f, 0f, 1f),
                rightShoulder = PoseLandmark(0.65f, 0.5f, 0f, 1f),
            ),
        )
        assertThat(result).contains(PoseSuggestion.RELAX_SHOULDERS)
    }

    @Test
    fun `nose too close to shoulder line triggers RAISE_CHIN`() {
        val result = useCase(
            landmarks(
                nose = PoseLandmark(0.5f, 0.38f, 0f, 1f),
                leftShoulder = PoseLandmark(0.35f, 0.4f, 0f, 1f),
                rightShoulder = PoseLandmark(0.65f, 0.4f, 0f, 1f),
            ),
        )
        assertThat(result).contains(PoseSuggestion.RAISE_CHIN)
    }

    @Test
    fun `low visibility shoulders return NONE`() {
        val result = useCase(
            landmarks(
                nose = PoseLandmark(0.5f, 0.2f, 0f, 1f),
                leftShoulder = PoseLandmark(0.35f, 0.3f, 0f, 0.1f),
                rightShoulder = PoseLandmark(0.65f, 0.5f, 0f, 0.1f),
            ),
        )
        assertThat(result).containsExactly(PoseSuggestion.NONE)
    }

    @Test
    fun `too few landmarks returns NONE`() {
        assertThat(useCase(emptyList())).containsExactly(PoseSuggestion.NONE)
    }
}
