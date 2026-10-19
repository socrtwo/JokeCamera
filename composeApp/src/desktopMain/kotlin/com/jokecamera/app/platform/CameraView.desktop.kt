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
    // Desktop doesn't have camera access built-in
    // Shows a gradient background with the app working as a joke-telling app
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0d1b2a), Color(0xFF1b2838), Color(0xFF2a3f54))
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
                text = getPlatformName(),
                color = Color.White.copy(alpha = 0.15f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
