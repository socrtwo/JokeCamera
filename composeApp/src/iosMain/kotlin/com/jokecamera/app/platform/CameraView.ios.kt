package com.jokecamera.app.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.jokecamera.app.model.DetectionMode

@Composable
actual fun PlatformCameraPreview(
    modifier: Modifier,
    useFrontCamera: Boolean,
    enableFaceDetection: Boolean,
    detectionMode: DetectionMode,
    captureController: CaptureController,
    faceDetectionListener: FaceDetectionListener?,
    onPhotoCaptured: (success: Boolean) -> Unit
) {
    // iOS camera preview - placeholder with gradient background
    // Full camera integration would use AVCaptureSession via UIViewRepresentable
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Joke Camera\niOS",
            color = Color.White.copy(alpha = 0.3f),
            textAlign = TextAlign.Center
        )
    }
}
