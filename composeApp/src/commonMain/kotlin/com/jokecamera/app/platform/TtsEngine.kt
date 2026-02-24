package com.jokecamera.app.platform

/**
 * Cross-platform Text-to-Speech engine interface.
 * Implementations use Android TTS, AVSpeechSynthesizer on iOS,
 * system TTS on desktop, and Web Speech API on web.
 */
enum class QueueMode { FLUSH, ADD }

interface TtsEngine {
    val isReady: Boolean
    fun initialize(onReady: () -> Unit, onError: (String) -> Unit)
    fun speak(text: String, queueMode: QueueMode, utteranceId: String)
    fun speakSilence(durationMs: Long, queueMode: QueueMode, utteranceId: String)
    fun stop()
    fun shutdown()
    fun setOnUtteranceCompleted(callback: (String) -> Unit)
}

expect fun createTtsEngine(): TtsEngine
