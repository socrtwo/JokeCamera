package com.jokecamera.app.platform

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.jokecamera.app.model.DetectionMode
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val mediaSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    // Set up capture controller
    DisposableEffect(captureController, imageCapture) {
        captureController.onCaptureRequested = {
            val capture = imageCapture ?: return@onCaptureRequested

            mediaSound.play(MediaActionSound.SHUTTER_CLICK)

            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "JokeCamera_$name")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JokeCamera")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Toast.makeText(context, "Photo saved!", Toast.LENGTH_SHORT).show()
                        onPhotoCaptured(true)
                    }
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CameraPreview", "Photo capture failed: ${exc.message}", exc)
                        onPhotoCaptured(false)
                    }
                }
            )
        }
        onDispose {
            captureController.onCaptureRequested = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            mediaSound.release()
        }
    }

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    if (!hasCameraPermission) {
        // Show a message that camera permission is needed
        androidx.compose.foundation.layout.Box(modifier = modifier) {
            androidx.compose.material3.Text(
                "Camera permission required",
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                val lensFacing = if (useFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                val useCases = mutableListOf<UseCase>(preview, capture)

                if (enableFaceDetection && faceDetectionListener != null) {
                    val faceDetectorOptions = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build()
                    val faceDetector = FaceDetection.getClient(faceDetectorOptions)

                    var hasTriggeredThisCycle = false
                    var smileDetectedForAndMode = false

                    val smileThreshold = 0.4f
                    val laughThreshold = 0.75f
                    val minEyeOpen = 0.3f

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @OptIn(ExperimentalGetImage::class)
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage, imageProxy.imageInfo.rotationDegrees
                                    )
                                    faceDetector.process(inputImage)
                                        .addOnSuccessListener { faces ->
                                            if (faces.isEmpty()) {
                                                faceDetectionListener.onNoFaceDetected()
                                            } else {
                                                faceDetectionListener.onFaceDetected(faces.size)
                                                if (!hasTriggeredThisCycle) {
                                                    for (face in faces) {
                                                        val sp = face.smilingProbability ?: 0f
                                                        val leo = face.leftEyeOpenProbability ?: 1f
                                                        val reo = face.rightEyeOpenProbability ?: 1f
                                                        if (leo < minEyeOpen && reo < minEyeOpen) continue
                                                        val isSmiling = sp >= smileThreshold
                                                        val isLaughing = sp >= laughThreshold
                                                        when (detectionMode) {
                                                            DetectionMode.SMILE_ONLY -> if (isSmiling) {
                                                                hasTriggeredThisCycle = true
                                                                faceDetectionListener.onSmileDetected()
                                                            }
                                                            DetectionMode.LAUGH_ONLY -> if (isLaughing) {
                                                                hasTriggeredThisCycle = true
                                                                faceDetectionListener.onLaughDetected()
                                                            }
                                                            DetectionMode.SMILE_AND_LAUGH -> {
                                                                if (!smileDetectedForAndMode && isSmiling && !isLaughing) {
                                                                    smileDetectedForAndMode = true
                                                                    faceDetectionListener.onSmileDetected()
                                                                } else if (smileDetectedForAndMode && isLaughing) {
                                                                    hasTriggeredThisCycle = true
                                                                    faceDetectionListener.onLaughDetected()
                                                                }
                                                            }
                                                            DetectionMode.SMILE_OR_LAUGH -> {
                                                                if (isLaughing) {
                                                                    hasTriggeredThisCycle = true
                                                                    faceDetectionListener.onLaughDetected()
                                                                } else if (isSmiling) {
                                                                    hasTriggeredThisCycle = true
                                                                    faceDetectionListener.onSmileDetected()
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    useCases.add(imageAnalysis)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        *useCases.toTypedArray()
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
