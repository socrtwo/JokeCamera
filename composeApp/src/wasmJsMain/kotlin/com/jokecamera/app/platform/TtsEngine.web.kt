package com.jokecamera.app.platform

import kotlinx.browser.window

/**
 * Web TTS engine using the Web Speech API (SpeechSynthesis).
 */
class WebTtsEngine : TtsEngine {
    private var _isReady = false
    private var utteranceCallback: ((String) -> Unit)? = null

    override val isReady: Boolean get() = _isReady

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        _isReady = true
        onReady()
    }

    override fun speak(text: String, queueMode: QueueMode, utteranceId: String) {
        val synth = window.asDynamic().speechSynthesis
        if (synth == null || synth == undefined) {
            utteranceCallback?.invoke(utteranceId)
            return
        }

        if (queueMode == QueueMode.FLUSH) {
            synth.cancel()
        }

        val utterance = js("new SpeechSynthesisUtterance(text)")
        utterance.lang = "en-US"
        utterance.rate = 1.0
        utterance.onend = {
            utteranceCallback?.invoke(utteranceId)
        }
        synth.speak(utterance)
    }

    override fun speakSilence(durationMs: Long, queueMode: QueueMode, utteranceId: String) {
        window.setTimeout({
            utteranceCallback?.invoke(utteranceId)
        }, durationMs.toInt())
    }

    override fun stop() {
        val synth = window.asDynamic().speechSynthesis
        synth?.cancel()
    }

    override fun shutdown() {
        stop()
        _isReady = false
    }

    override fun setOnUtteranceCompleted(callback: (String) -> Unit) {
        utteranceCallback = callback
    }
}

actual fun createTtsEngine(): TtsEngine = WebTtsEngine()
