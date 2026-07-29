package com.framewise.feature.camerapreview

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Thin wrapper around Android's [TextToSpeech]. Debouncing (only speak when
 * the guidance actually changes, with a minimum interval) is the caller's
 * job — see [CameraPreviewViewModel] — this class only owns the engine
 * lifecycle.
 */
class VoiceGuidanceSpeaker @Inject constructor(
    @ApplicationContext context: Context,
) {
    private var isReady = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts.language = Locale("vi", "VN")
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "framewise-guidance")
    }

    fun shutdown() {
        if (!::tts.isInitialized) return
        tts.stop()
        tts.shutdown()
    }
}
