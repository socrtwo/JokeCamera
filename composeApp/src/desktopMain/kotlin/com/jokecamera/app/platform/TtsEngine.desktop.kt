package com.jokecamera.app.platform

import kotlinx.coroutines.*

/**
 * Desktop TTS engine using system commands:
 * - macOS: say
 * - Linux: espeak / spd-say
 * - Windows: PowerShell SpeechSynthesizer
 */
class DesktopTtsEngine : TtsEngine {
    private var _isReady = false
    private var utteranceCallback: ((String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null
    private val os = System.getProperty("os.name").lowercase()

    override val isReady: Boolean get() = _isReady

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        _isReady = true
        onReady()
    }

    override fun speak(text: String, queueMode: QueueMode, utteranceId: String) {
        val job = scope.launch {
            if (queueMode == QueueMode.FLUSH) {
                currentJob?.cancelAndJoin()
            }
            try {
                val cleanText = text.replace("\"", "\\\"").replace("'", "\\'")
                val process = when {
                    os.contains("mac") || os.contains("darwin") -> {
                        ProcessBuilder("say", text).start()
                    }
                    os.contains("win") -> {
                        ProcessBuilder(
                            "powershell", "-Command",
                            "Add-Type -AssemblyName System.Speech; " +
                            "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('$cleanText')"
                        ).start()
                    }
                    else -> {
                        // Linux - try espeak first, then spd-say
                        try {
                            ProcessBuilder("espeak", text).start()
                        } catch (e: Exception) {
                            try {
                                ProcessBuilder("spd-say", text).start()
                            } catch (e2: Exception) {
                                null
                            }
                        }
                    }
                }
                process?.waitFor()
                utteranceCallback?.invoke(utteranceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                utteranceCallback?.invoke(utteranceId)
            }
        }
        currentJob = job
    }

    override fun speakSilence(durationMs: Long, queueMode: QueueMode, utteranceId: String) {
        val job = scope.launch {
            delay(durationMs)
            utteranceCallback?.invoke(utteranceId)
        }
        currentJob = job
    }

    override fun stop() {
        currentJob?.cancel()
    }

    override fun shutdown() {
        stop()
        scope.cancel()
        _isReady = false
    }

    override fun setOnUtteranceCompleted(callback: (String) -> Unit) {
        utteranceCallback = callback
    }
}

actual fun createTtsEngine(): TtsEngine = DesktopTtsEngine()
