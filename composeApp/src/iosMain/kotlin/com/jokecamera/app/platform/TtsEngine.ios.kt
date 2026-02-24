package com.jokecamera.app.platform

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

class IosTtsEngine : TtsEngine {
    private val synthesizer = AVSpeechSynthesizer()
    private var _isReady = false
    private var utteranceCallback: ((String) -> Unit)? = null
    private var currentUtteranceId: String = ""

    // Map utterances to IDs
    private val utteranceIds = mutableMapOf<AVSpeechUtterance, String>()

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            val id = utteranceIds.remove(didFinishSpeechUtterance) ?: ""
            utteranceCallback?.invoke(id)
        }
    }

    override val isReady: Boolean get() = _isReady

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        synthesizer.delegate = delegate
        _isReady = true
        onReady()
    }

    override fun speak(text: String, queueMode: QueueMode, utteranceId: String) {
        if (queueMode == QueueMode.FLUSH) {
            synthesizer.stopSpeakingAtBoundary(0) // AVSpeechBoundaryImmediate
            utteranceIds.clear()
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.rate = 0.5f
        utteranceIds[utterance] = utteranceId
        synthesizer.speakUtterance(utterance)
    }

    override fun speakSilence(durationMs: Long, queueMode: QueueMode, utteranceId: String) {
        // iOS doesn't have built-in silence utterance; use a very quiet utterance
        val utterance = AVSpeechUtterance.speechUtteranceWithString(" ")
        utterance.rate = 0.5f
        utterance.preUtteranceDelay = durationMs / 1000.0
        utteranceIds[utterance] = utteranceId
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(0)
        utteranceIds.clear()
    }

    override fun shutdown() {
        stop()
        _isReady = false
    }

    override fun setOnUtteranceCompleted(callback: (String) -> Unit) {
        utteranceCallback = callback
    }
}

actual fun createTtsEngine(): TtsEngine = IosTtsEngine()
