package com.framewise.domain.usecase

import com.framewise.domain.model.SceneType
import com.framewise.domain.model.ShootingMode
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

    @Test
    fun `a manually chosen mode overrides a conflicting auto-detected scene`() {
        // User picked PORTRAIT but ML Kit's scene classifier currently
        // reports PET (e.g. a pet briefly walked through frame) - the tip
        // must still match the mode the user explicitly chose, not the
        // auto-detected scene, so it never contradicts their selection.
        val tip = useCase(scene = SceneType.PET, mode = ShootingMode.PORTRAIT)

        assertThat(tip).contains("mắt")
        assertThat(tip).doesNotContain("thú cưng")
    }

    @Test
    fun `AUTO mode falls back to the auto-detected scene's tip`() {
        assertThat(useCase(scene = SceneType.PET, mode = ShootingMode.AUTO)).contains("thú cưng")
    }
}
