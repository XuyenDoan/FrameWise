package com.framewise.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlashModeTest {

    @Test
    fun `next cycles OFF to AUTO to ON and back to OFF`() {
        assertThat(FlashMode.OFF.next()).isEqualTo(FlashMode.AUTO)
        assertThat(FlashMode.AUTO.next()).isEqualTo(FlashMode.ON)
        assertThat(FlashMode.ON.next()).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun `next from TORCH returns to OFF`() {
        assertThat(FlashMode.TORCH.next()).isEqualTo(FlashMode.OFF)
    }
}
