package com.framewise.domain.model

enum class FlashMode {
    OFF,
    ON,
    AUTO,
    TORCH,
    ;

    /** Cycles OFF -> AUTO -> ON -> OFF for the flash toggle control (TORCH is selected separately). */
    fun next(): FlashMode = when (this) {
        OFF -> AUTO
        AUTO -> ON
        ON -> OFF
        TORCH -> OFF
    }
}
