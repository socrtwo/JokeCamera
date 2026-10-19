package com.jokecamera.app.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Web platform - gradient background placeholder
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Joke Camera",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Web Edition",
                color = Color.White.copy(alpha = 0.15f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
