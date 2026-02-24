package com.jokecamera.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jokecamera.app.model.DetectionMode

/**
 * Controller that allows triggering photo capture from shared code.
 */
class CaptureController {
    var onCaptureRequested: (() -> Unit)? = null

    fun capture() {
        onCaptureRequested?.invoke()
    }
}

/**
 * Face detection callback interface for cross-platform use.
 */
interface FaceDetectionListener {
    fun onSmileDetected()
    fun onLaughDetected()
    fun onFaceDetected(count: Int)
    fun onNoFaceDetected()
}

/**
 * Platform-specific camera preview composable.
 * On platforms without camera support, displays a placeholder.
 */
@Composable
expect fun PlatformCameraPreview(
    modifier: Modifier,
    useFrontCamera: Boolean,
    enableFaceDetection: Boolean,
    detectionMode: DetectionMode,
    captureController: CaptureController,
    faceDetectionListener: FaceDetectionListener?,
    onPhotoCaptured: (success: Boolean) -> Unit
)
