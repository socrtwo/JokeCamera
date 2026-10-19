package com.jokecamera.app.platform

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

private var appContext: android.content.Context? = null

fun initTtsContext(context: android.content.Context) {
    appContext = context.applicationContext
}

class AndroidTtsEngine : TtsEngine {
    private var tts: TextToSpeech? = null
    private var _isReady = false
    private var utteranceCallback: ((String) -> Unit)? = null

    override val isReady: Boolean get() = _isReady

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        if (_isReady) {
            onReady()
            return
        }
        val ctx = appContext ?: run {
            onError("Android context not initialized")
            return
        }
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onError("Language not supported")
                } else {
                    _isReady = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            utteranceId?.let { utteranceCallback?.invoke(it) }
                        }
                        override fun onError(utteranceId: String?) {}
                    })
                    onReady()
                }
            } else {
                onError("TTS initialization failed")
            }
        }
    }

    override fun speak(text: String, queueMode: QueueMode, utteranceId: String) {
        val mode = if (queueMode == QueueMode.FLUSH) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, mode, null, utteranceId)
    }

    override fun speakSilence(durationMs: Long, queueMode: QueueMode, utteranceId: String) {
        val mode = if (queueMode == QueueMode.FLUSH) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.playSilentUtterance(durationMs, mode, utteranceId)
    }

    override fun stop() { tts?.stop() }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        _isReady = false
    }

    override fun setOnUtteranceCompleted(callback: (String) -> Unit) {
        utteranceCallback = callback
    }
}

actual fun createTtsEngine(): TtsEngine = AndroidTtsEngine()
