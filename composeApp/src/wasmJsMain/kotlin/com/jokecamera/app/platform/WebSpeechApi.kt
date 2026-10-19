@file:kotlin.js.JsQualifier("window")

package com.jokecamera.app.platform

import kotlin.js.JsAny

external val speechSynthesis: SpeechSynthesis?

external fun setTimeout(handler: (JsAny?) -> Unit, timeout: Int): Int

external class SpeechSynthesisUtterance(text: String) : JsAny {
    var lang: String
    var rate: Double
    var onend: ((JsAny?) -> Unit)?
}

external interface SpeechSynthesis : JsAny {
    fun cancel()
    fun speak(utterance: SpeechSynthesisUtterance)
}
