package com.jokecamera.app.platform

actual fun getPlatformType(): PlatformType = PlatformType.ANDROID
actual fun getPlatformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"
