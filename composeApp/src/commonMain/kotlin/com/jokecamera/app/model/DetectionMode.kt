package com.jokecamera.app.model

enum class DetectionMode {
    SMILE_ONLY,
    LAUGH_ONLY,
    SMILE_AND_LAUGH,
    SMILE_OR_LAUGH;

    fun displayName(): String = when (this) {
        SMILE_ONLY -> "Wait for smile only"
        LAUGH_ONLY -> "Wait for laugh only (big smile)"
        SMILE_AND_LAUGH -> "Wait for both smile AND laugh"
        SMILE_OR_LAUGH -> "Wait for smile OR laugh (default)"
    }

    companion object {
        fun fromInt(value: Int): DetectionMode = when (value) {
            0 -> SMILE_ONLY
            1 -> LAUGH_ONLY
            2 -> SMILE_AND_LAUGH
            3 -> SMILE_OR_LAUGH
            else -> SMILE_OR_LAUGH
        }
    }
}
