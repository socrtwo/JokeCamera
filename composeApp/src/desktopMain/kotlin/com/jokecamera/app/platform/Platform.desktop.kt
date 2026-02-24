package com.jokecamera.app.platform

actual fun getPlatformType(): PlatformType = PlatformType.DESKTOP

actual fun getPlatformName(): String {
    val os = System.getProperty("os.name") ?: "Desktop"
    val version = System.getProperty("os.version") ?: ""
    return "$os $version"
}
