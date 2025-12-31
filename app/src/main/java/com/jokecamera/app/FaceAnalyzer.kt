package com.jokecamera.app

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Detection mode options for smile/laugh detection
 */
enum class DetectionMode {
    SMILE_ONLY,       // Only detect smiles
    LAUGH_ONLY,       // Only detect laughs (big smiles)
    SMILE_AND_LAUGH,  // Require both smile AND laugh (same person showing progression)
    SMILE_OR_LAUGH    // Detect either smile OR laugh (default)
}

/**
 * Callback interface for face detection results
 */
interface FaceDetectionCallback {
    fun onSmileDetected()
    fun onLaughDetected()
    fun onFaceDetected(faceCount: Int)
    fun onNoFaceDetected()
}

/**
 * Analyzes camera frames for face detection using ML Kit.
 * Detects smiles and laughs based on configurable thresholds.
 */
class FaceAnalyzer(
    private val callback: FaceDetectionCallback,
    private var detectionMode: DetectionMode = DetectionMode.SMILE_OR_LAUGH
) : ImageAnalysis.Analyzer {
    
    companion object {
        private const val TAG = "FaceAnalyzer"
        
        // Threshold for smile detection (0.0 to 1.0)
        // Lower values = more sensitive to smiles
        private const val SMILE_THRESHOLD = 0.4f
        
        // Threshold for laugh detection (0.0 to 1.0)
        // Higher threshold means bigger smile = laugh
        private const val LAUGH_THRESHOLD = 0.75f
        
        // Minimum eye open probability to consider face valid
        private const val MIN_EYE_OPEN = 0.3f
    }
    
    private val faceDetector: FaceDetector
    
    // Track if we've already triggered detection in current analysis cycle
    private var hasTriggeredThisCycle = false
    
    // For SMILE_AND_LAUGH mode, track if smile was detected first
    private var smileDetectedForAndMode = false
    
    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        
        faceDetector = FaceDetection.getClient(options)
    }
    
    /**
     * Update the detection mode
     */
    fun setDetectionMode(mode: DetectionMode) {
        detectionMode = mode
        resetDetectionState()
    }
    
    /**
     * Reset detection state for new joke cycle
     */
    fun resetDetectionState() {
        hasTriggeredThisCycle = false
        smileDetectedForAndMode = false
    }
    
    /**
     * Allow triggering again after photo is taken
     */
    fun allowNewDetection() {
        hasTriggeredThisCycle = false
    }
    
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                processFaces(faces)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    private fun processFaces(faces: List<Face>) {
        if (faces.isEmpty()) {
            callback.onNoFaceDetected()
            return
        }
        
        callback.onFaceDetected(faces.size)
        
        // Don't trigger again if already triggered this cycle
        if (hasTriggeredThisCycle) {
            return
        }
        
        // Check each face for smile/laugh
        for (face in faces) {
            val smileProbability = face.smilingProbability ?: 0f
            val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
            val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
            
            // Skip if eyes are closed (might be blinking)
            if (leftEyeOpen < MIN_EYE_OPEN && rightEyeOpen < MIN_EYE_OPEN) {
                continue
            }
            
            val isSmiling = smileProbability >= SMILE_THRESHOLD
            val isLaughing = smileProbability >= LAUGH_THRESHOLD
            
            Log.d(TAG, "Face detected - Smile probability: $smileProbability, " +
                    "Is smiling: $isSmiling, Is laughing: $isLaughing")
            
            when (detectionMode) {
                DetectionMode.SMILE_ONLY -> {
                    if (isSmiling) {
                        hasTriggeredThisCycle = true
                        callback.onSmileDetected()
                        return
                    }
                }
                
                DetectionMode.LAUGH_ONLY -> {
                    if (isLaughing) {
                        hasTriggeredThisCycle = true
                        callback.onLaughDetected()
                        return
                    }
                }
                
                DetectionMode.SMILE_AND_LAUGH -> {
                    // First detect smile, then detect laugh
                    if (!smileDetectedForAndMode && isSmiling && !isLaughing) {
                        smileDetectedForAndMode = true
                        callback.onSmileDetected()
                    } else if (smileDetectedForAndMode && isLaughing) {
                        hasTriggeredThisCycle = true
                        callback.onLaughDetected()
                        return
                    }
                }
                
                DetectionMode.SMILE_OR_LAUGH -> {
                    if (isLaughing) {
                        hasTriggeredThisCycle = true
                        callback.onLaughDetected()
                        return
                    } else if (isSmiling) {
                        hasTriggeredThisCycle = true
                        callback.onSmileDetected()
                        return
                    }
                }
            }
        }
    }
    
    /**
     * Release resources
     */
    fun shutdown() {
        faceDetector.close()
    }
}
