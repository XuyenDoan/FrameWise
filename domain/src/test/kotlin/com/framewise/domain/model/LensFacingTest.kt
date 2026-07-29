package com.framewise.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LensFacingTest {

    @Test
    fun `opposite of BACK is FRONT and vice versa`() {
        assertThat(LensFacing.BACK.opposite()).isEqualTo(LensFacing.FRONT)
        assertThat(LensFacing.FRONT.opposite()).isEqualTo(LensFacing.BACK)
    }
}
