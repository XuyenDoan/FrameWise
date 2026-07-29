package com.framewise.domain.usecase

import com.framewise.domain.model.SceneType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GetPhotographyTipsUseCaseTest {

    private val useCase = GetPhotographyTipsUseCase()

    @Test
    fun `every scene type returns a non-blank tip`() {
        SceneType.entries.forEach { scene ->
            assertThat(useCase(scene).isNotBlank()).isTrue()
        }
    }

    @Test
    fun `portrait tip mentions eyes`() {
        assertThat(useCase(SceneType.PORTRAIT)).contains("mắt")
    }
}
