package com.jokecamera.app

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Main Activity for JokeCamera app.
 * Features:
 * - Camera preview with front/back camera switching
 * - Text-to-Speech joke telling with configurable timing
 * - ML Kit face detection for smile/laugh detection
 * - Automatic photo capture on smile/laugh or timer
 * - Manual joke telling mode
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, FaceDetectionCallback {
    
    companion object {
        private const val TAG = "JokeCamera"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    
    // Camera components
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var faceAnalyzer: FaceAnalyzer? = null
    
    // TTS
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    
    // Managers and handlers
    private lateinit var jokeManager: JokeManager
    private val handler = Handler(Looper.getMainLooper())
    private var mediaActionSound: MediaActionSound? = null
    
    // UI Elements
    private lateinit var buttonTellJoke: Button
    private lateinit var buttonCapture: ImageButton
    private lateinit var buttonSwitchCamera: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var buttonStartStop: Button
    private lateinit var textJokeSetup: TextView
    private lateinit var textJokePunchline: TextView
    private lateinit var textStatus: TextView
    private lateinit var textFaceStatus: TextView
    
    // State variables
    private var isAutoMode = false
    private var isWaitingForReaction = false
    private var currentJoke: Joke? = null
    private var photoTaken = false
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeManagers()
        setupClickListeners()
        
        // Request permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        
        // Initialize TTS
        tts = TextToSpeech(this, this)
        
        // Initialize camera shutter sound
        mediaActionSound = MediaActionSound()
        mediaActionSound?.load(MediaActionSound.SHUTTER_CLICK)
    }
    
    private fun initializeViews() {
        previewView = findViewById(R.id.preview_view)
        buttonTellJoke = findViewById(R.id.button_tell_joke)
        buttonCapture = findViewById(R.id.button_capture)
        buttonSwitchCamera = findViewById(R.id.button_switch_camera)
        buttonSettings = findViewById(R.id.button_settings)
        buttonStartStop = findViewById(R.id.button_start_stop)
        textJokeSetup = findViewById(R.id.text_joke_setup)
        textJokePunchline = findViewById(R.id.text_joke_punchline)
        textStatus = findViewById(R.id.text_status)
        textFaceStatus = findViewById(R.id.text_face_status)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun initializeManagers() {
        jokeManager = JokeManager(this)
    }
    
    private fun setupClickListeners() {
        buttonTellJoke.setOnClickListener {
            if (ttsInitialized) {
                tellJoke()
            } else {
                Toast.makeText(this, "Speech not ready yet", Toast.LENGTH_SHORT).show()
            }
        }
        
        buttonCapture.setOnClickListener {
            takePhoto()
        }
        
        buttonSwitchCamera.setOnClickListener {
            switchCamera()
        }
        
        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        buttonStartStop.setOnClickListener {
            toggleAutoMode()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUIFromSettings()
        
        // Restart camera if settings changed
        if (cameraProvider != null) {
            bindCameraUseCases()
        }
    }
    
    private fun updateUIFromSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val manualMode = prefs.getBoolean(SettingsActivity.KEY_MANUAL_JOKE, false)
        
        // Show/hide manual joke button based on settings
        buttonTellJoke.visibility = if (manualMode) View.VISIBLE else View.GONE
        
        // Update detection mode in face analyzer
        val detectionModeInt = prefs.getInt(SettingsActivity.KEY_DETECTION_MODE, 3)
        val detectionMode = when (detectionModeInt) {
            0 -> DetectionMode.SMILE_ONLY
            1 -> DetectionMode.LAUGH_ONLY
            2 -> DetectionMode.SMILE_AND_LAUGH
            3 -> DetectionMode.SMILE_OR_LAUGH
            else -> DetectionMode.SMILE_OR_LAUGH
        }
        faceAnalyzer?.setDetectionMode(detectionMode)
        
        // Update remaining jokes count
        val remaining = jokeManager.getRemainingCount()
        textStatus.text = "Ready - $remaining jokes remaining"
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val detectionEnabled = prefs.getBoolean(SettingsActivity.KEY_DETECTION_ENABLED, true)
        val detectionModeInt = prefs.getInt(SettingsActivity.KEY_DETECTION_MODE, 3)
        val detectionMode = when (detectionModeInt) {
            0 -> DetectionMode.SMILE_ONLY
            1 -> DetectionMode.LAUGH_ONLY
            2 -> DetectionMode.SMILE_AND_LAUGH
            3 -> DetectionMode.SMILE_OR_LAUGH
            else -> DetectionMode.SMILE_OR_LAUGH
        }
        
        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image capture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        // Face analyzer
        faceAnalyzer?.shutdown()
        faceAnalyzer = FaceAnalyzer(this, detectionMode)
        
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                if (detectionEnabled) {
                    it.setAnalyzer(cameraExecutor, faceAnalyzer!!)
                }
            }
        
        // Camera selector
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        try {
            cameraProvider.unbindAll()
            
            val useCases = mutableListOf<UseCase>(preview, imageCapture!!)
            if (detectionEnabled) {
                useCases.add(imageAnalyzer!!)
            }
            
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                *useCases.toTypedArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }
    
    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases()
    }
    
    private fun toggleAutoMode() {
        isAutoMode = !isAutoMode
        
        if (isAutoMode) {
            buttonStartStop.text = "Stop"
            buttonTellJoke.isEnabled = false
            buttonCapture.isEnabled = false
            textStatus.text = "Auto mode - Starting..."
            
            // Start telling jokes automatically
            handler.postDelayed({ tellJoke() }, 1000)
        } else {
            buttonStartStop.text = "Start Auto"
            buttonTellJoke.isEnabled = true
            buttonCapture.isEnabled = true
            isWaitingForReaction = false
            textStatus.text = "Stopped"
            
            // Cancel any pending actions
            handler.removeCallbacksAndMessages(null)
            tts?.stop()
        }
    }
    
    private fun tellJoke() {
        if (!ttsInitialized) {
            Toast.makeText(this, "Speech not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get next joke
        currentJoke = jokeManager.getNextJoke()
        val joke = currentJoke ?: return
        
        photoTaken = false
        isWaitingForReaction = false
        faceAnalyzer?.resetDetectionState()
        
        // Display setup
        textJokeSetup.text = joke.setup
        textJokePunchline.text = ""
        textStatus.text = "Telling joke..."
        
        // Get punchline delay from settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val punchlineDelay = prefs.getFloat(
            SettingsActivity.KEY_PUNCHLINE_DELAY,
            SettingsActivity.DEFAULT_PUNCHLINE_DELAY
        )
        
        // Speak setup
        tts?.speak(joke.setup, TextToSpeech.QUEUE_FLUSH, null, "setup")
        
        // Schedule punchline after configurable delay
        tts?.playSilentUtterance((punchlineDelay * 1000).toLong(), TextToSpeech.QUEUE_ADD, "pause")
        
        // Speak punchline
        val punchlineParams = Bundle()
        tts?.speak(joke.punchline, TextToSpeech.QUEUE_ADD, punchlineParams, "punchline")
    }
    
    private fun onJokeComplete() {
        val joke = currentJoke ?: return
        
        textJokePunchline.text = joke.punchline
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val detectionEnabled = prefs.getBoolean(SettingsActivity.KEY_DETECTION_ENABLED, true)
        val timerMode = prefs.getBoolean(SettingsActivity.KEY_TIMER_MODE, false)
        
        if (timerMode) {
            // Timer mode - take photo after delay
            val timerDelay = prefs.getFloat(
                SettingsActivity.KEY_TIMER_DELAY,
                SettingsActivity.DEFAULT_TIMER_DELAY
            )
            textStatus.text = "Taking photo in ${timerDelay}s..."
            
            handler.postDelayed({
                if (!photoTaken) {
                    takePhoto()
                }
            }, (timerDelay * 1000).toLong())
        } else if (detectionEnabled) {
            // Detection mode - wait for smile/laugh
            isWaitingForReaction = true
            faceAnalyzer?.allowNewDetection()
            textStatus.text = "Waiting for smile/laugh..."
            
            // Set timeout to tell another joke
            val nextJokeWait = prefs.getFloat(
                SettingsActivity.KEY_NEXT_JOKE_WAIT,
                SettingsActivity.DEFAULT_NEXT_JOKE_WAIT
            )
            
            handler.postDelayed({
                if (!photoTaken && isAutoMode) {
                    textStatus.text = "No reaction - telling another joke..."
                    isWaitingForReaction = false
                    handler.postDelayed({ tellJoke() }, 500)
                }
            }, (nextJokeWait * 1000).toLong())
        } else {
            // Manual mode - just wait
            textStatus.text = "Joke told - capture manually"
            isWaitingForReaction = false
            
            if (isAutoMode) {
                val nextJokeWait = prefs.getFloat(
                    SettingsActivity.KEY_NEXT_JOKE_WAIT,
                    SettingsActivity.DEFAULT_NEXT_JOKE_WAIT
                )
                handler.postDelayed({
                    if (!photoTaken) {
                        tellJoke()
                    }
                }, (nextJokeWait * 1000).toLong())
            }
        }
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        if (photoTaken) return
        photoTaken = true
        isWaitingForReaction = false
        
        // Cancel any pending joke telling
        handler.removeCallbacksAndMessages(null)
        
        // Play shutter sound
        mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)
        
        // Create file name
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
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo saved!"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    
                    textStatus.text = "Photo captured!"
                    
                    // In auto mode, continue with next joke after a delay
                    if (isAutoMode) {
                        handler.postDelayed({
                            photoTaken = false
                            tellJoke()
                        }, 2000)
                    } else {
                        val remaining = jokeManager.getRemainingCount()
                        handler.postDelayed({
                            textStatus.text = "Ready - $remaining jokes remaining"
                            photoTaken = false
                        }, 2000)
                    }
                }
                
                override fun onError(e: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${e.message}", e)
                    Toast.makeText(baseContext, "Photo capture failed", Toast.LENGTH_SHORT).show()
                    textStatus.text = "Capture failed"
                    photoTaken = false
                }
            }
        )
    }
    
    // TextToSpeech.OnInitListener
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language not supported")
                Toast.makeText(this, "Speech language not supported", Toast.LENGTH_LONG).show()
            } else {
                ttsInitialized = true
                Log.d(TAG, "TTS initialized successfully")
                
                // Set up utterance listener
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS started: $utteranceId")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS done: $utteranceId")
                        if (utteranceId == "punchline") {
                            handler.post { onJokeComplete() }
                        }
                    }
                    
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS error: $utteranceId")
                    }
                })
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
            Toast.makeText(this, "Speech initialization failed", Toast.LENGTH_LONG).show()
        }
    }
    
    // FaceDetectionCallback
    override fun onSmileDetected() {
        runOnUiThread {
            Log.d(TAG, "Smile detected!")
            textFaceStatus.text = "😊 Smile detected!"
            
            if (isWaitingForReaction && !photoTaken) {
                textStatus.text = "Smile detected - capturing!"
                handler.postDelayed({ takePhoto() }, 100)
            }
        }
    }
    
    override fun onLaughDetected() {
        runOnUiThread {
            Log.d(TAG, "Laugh detected!")
            textFaceStatus.text = "😂 Laugh detected!"
            
            if (isWaitingForReaction && !photoTaken) {
                textStatus.text = "Laugh detected - capturing!"
                handler.postDelayed({ takePhoto() }, 100)
            }
        }
    }
    
    override fun onFaceDetected(faceCount: Int) {
        runOnUiThread {
            if (!isWaitingForReaction) {
                textFaceStatus.text = "👤 $faceCount face${if (faceCount > 1) "s" else ""} detected"
            }
        }
    }
    
    override fun onNoFaceDetected() {
        runOnUiThread {
            textFaceStatus.text = "No face detected"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tts?.stop()
        tts?.shutdown()
        faceAnalyzer?.shutdown()
        mediaActionSound?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
