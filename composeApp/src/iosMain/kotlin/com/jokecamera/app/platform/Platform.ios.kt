package com.jokecamera.app.platform

import platform.UIKit.UIDevice

actual fun getPlatformType(): PlatformType = PlatformType.IOS
actual fun getPlatformName(): String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
