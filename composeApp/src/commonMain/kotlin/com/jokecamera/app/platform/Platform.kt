package com.jokecamera.app.platform

enum class PlatformType {
    ANDROID, IOS, DESKTOP, WEB
}

expect fun getPlatformType(): PlatformType

expect fun getPlatformName(): String

/** Whether this platform supports camera capture */
fun supportsCameraCapture(): Boolean = when (getPlatformType()) {
    PlatformType.ANDROID, PlatformType.IOS -> true
    PlatformType.DESKTOP, PlatformType.WEB -> false
}

/** Whether this platform supports face detection */
fun supportsFaceDetection(): Boolean = when (getPlatformType()) {
    PlatformType.ANDROID -> true
    else -> false
}
