package com.jokecamera.app.model

object AppSettings {
    // Preference keys
    const val KEY_MANUAL_JOKE = "manual_joke_mode"
    const val KEY_DETECTION_ENABLED = "detection_enabled"
    const val KEY_TIMER_MODE = "timer_mode"
    const val KEY_TIMER_DELAY = "timer_delay"
    const val KEY_DETECTION_MODE = "detection_mode"
    const val KEY_NEXT_JOKE_WAIT = "next_joke_wait"
    const val KEY_PUNCHLINE_DELAY = "punchline_delay"
    const val KEY_CATEGORY_DAD = "category_dad"
    const val KEY_CATEGORY_GENERAL = "category_general"
    const val KEY_CATEGORY_KNOCK_KNOCK = "category_knock_knock"
    const val KEY_CATEGORY_PROGRAMMING = "category_programming"
    const val KEY_CATEGORY_CUSTOM = "category_custom"

    // Default values
    const val DEFAULT_MANUAL_JOKE = false
    const val DEFAULT_DETECTION_ENABLED = true
    const val DEFAULT_TIMER_MODE = false
    const val DEFAULT_TIMER_DELAY = 3.0f
    const val DEFAULT_DETECTION_MODE = 3
    const val DEFAULT_NEXT_JOKE_WAIT = 2.5f
    const val DEFAULT_PUNCHLINE_DELAY = 0.81f

    // Range limits
    const val MIN_PUNCHLINE_DELAY = 0.0f
    const val MAX_PUNCHLINE_DELAY = 2.0f
    const val MIN_NEXT_JOKE_WAIT = 0.5f
    const val MAX_NEXT_JOKE_WAIT = 10.0f
    const val MIN_TIMER_DELAY = 0.5f
    const val MAX_TIMER_DELAY = 10.0f
}
