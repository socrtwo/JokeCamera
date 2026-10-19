package com.jokecamera.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jokecamera.app.data.JokeRepository
import com.jokecamera.app.model.*
import com.jokecamera.app.platform.*
import com.jokecamera.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    jokeRepository: JokeRepository,
    settingsStore: SettingsStore,
    ttsEngine: TtsEngine,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val captureController = remember { CaptureController() }

    // State
    var jokeSetup by remember { mutableStateOf("") }
    var jokePunchline by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Ready") }
    var faceStatusText by remember { mutableStateOf("") }
    var isAutoMode by remember { mutableStateOf(false) }
    var isWaitingForReaction by remember { mutableStateOf(false) }
    var photoTaken by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var currentJoke by remember { mutableStateOf<Joke?>(null) }
    var ttsReady by remember { mutableStateOf(ttsEngine.isReady) }

    // Settings
    val manualMode = remember(Unit) { settingsStore.getBoolean(AppSettings.KEY_MANUAL_JOKE, AppSettings.DEFAULT_MANUAL_JOKE) }
    val detectionEnabled = remember(Unit) { settingsStore.getBoolean(AppSettings.KEY_DETECTION_ENABLED, AppSettings.DEFAULT_DETECTION_ENABLED) }
    val detectionModeInt = remember(Unit) { settingsStore.getInt(AppSettings.KEY_DETECTION_MODE, AppSettings.DEFAULT_DETECTION_MODE) }
    val detectionMode = DetectionMode.fromInt(detectionModeInt)

    // Update TTS ready state
    LaunchedEffect(Unit) {
        ttsEngine.initialize(
            onReady = { ttsReady = true },
            onError = { statusText = "Speech init failed" }
        )
        val remaining = jokeRepository.getRemainingCount()
        statusText = "Ready - $remaining jokes remaining"
    }

    // Face detection listener
    val faceListener = remember {
        object : FaceDetectionListener {
            override fun onSmileDetected() {
                faceStatusText = "Smile detected!"
                if (isWaitingForReaction && !photoTaken) {
                    statusText = "Smile detected - capturing!"
                    scope.launch {
                        delay(100)
                        captureController.capture()
                    }
                }
            }

            override fun onLaughDetected() {
                faceStatusText = "Laugh detected!"
                if (isWaitingForReaction && !photoTaken) {
                    statusText = "Laugh detected - capturing!"
                    scope.launch {
                        delay(100)
                        captureController.capture()
                    }
                }
            }

            override fun onFaceDetected(count: Int) {
                if (!isWaitingForReaction) {
                    faceStatusText = "$count face${if (count > 1) "s" else ""} detected"
                }
            }

            override fun onNoFaceDetected() {
                faceStatusText = "No face detected"
            }
        }
    }

    fun tellJoke() {
        if (!ttsReady) {
            statusText = "Speech not ready"
            return
        }

        val joke = jokeRepository.getNextJoke()
        currentJoke = joke
        photoTaken = false
        isWaitingForReaction = false

        jokeSetup = joke.setup
        jokePunchline = ""
        statusText = "Telling joke..."

        val punchlineDelay = settingsStore.getFloat(
            AppSettings.KEY_PUNCHLINE_DELAY,
            AppSettings.DEFAULT_PUNCHLINE_DELAY
        )

        ttsEngine.setOnUtteranceCompleted { utteranceId ->
            if (utteranceId == "punchline") {
                jokePunchline = joke.punchline

                val timerMode = settingsStore.getBoolean(AppSettings.KEY_TIMER_MODE, AppSettings.DEFAULT_TIMER_MODE)
                if (timerMode) {
                    val timerDelay = settingsStore.getFloat(AppSettings.KEY_TIMER_DELAY, AppSettings.DEFAULT_TIMER_DELAY)
                    statusText = "Taking photo in ${timerDelay}s..."
                    scope.launch {
                        delay((timerDelay * 1000).toLong())
                        if (!photoTaken) captureController.capture()
                    }
                } else if (detectionEnabled && supportsFaceDetection()) {
                    isWaitingForReaction = true
                    statusText = "Waiting for smile/laugh..."
                    val nextJokeWait = settingsStore.getFloat(AppSettings.KEY_NEXT_JOKE_WAIT, AppSettings.DEFAULT_NEXT_JOKE_WAIT)
                    scope.launch {
                        delay((nextJokeWait * 1000).toLong())
                        if (!photoTaken && isAutoMode) {
                            statusText = "No reaction - telling another joke..."
                            isWaitingForReaction = false
                            delay(500)
                            tellJoke()
                        }
                    }
                } else {
                    statusText = "Joke told!"
                    if (isAutoMode) {
                        val nextJokeWait = settingsStore.getFloat(AppSettings.KEY_NEXT_JOKE_WAIT, AppSettings.DEFAULT_NEXT_JOKE_WAIT)
                        scope.launch {
                            delay((nextJokeWait * 1000).toLong())
                            tellJoke()
                        }
                    }
                }
            }
        }

        ttsEngine.speak(joke.setup, QueueMode.FLUSH, "setup")
        ttsEngine.speakSilence((punchlineDelay * 1000).toLong(), QueueMode.ADD, "pause")
        ttsEngine.speak(joke.punchline, QueueMode.ADD, "punchline")
    }

    // Main layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (or placeholder on non-camera platforms)
        PlatformCameraPreview(
            modifier = Modifier.fillMaxSize(),
            useFrontCamera = useFrontCamera,
            enableFaceDetection = detectionEnabled,
            detectionMode = detectionMode,
            captureController = captureController,
            faceDetectionListener = faceListener,
            onPhotoCaptured = { success ->
                photoTaken = true
                statusText = if (success) "Photo captured!" else "Capture failed"
                if (isAutoMode && success) {
                    scope.launch {
                        delay(2000)
                        photoTaken = false
                        tellJoke()
                    }
                } else {
                    scope.launch {
                        delay(2000)
                        val remaining = jokeRepository.getRemainingCount()
                        statusText = "Ready - $remaining jokes remaining"
                        photoTaken = false
                    }
                }
            }
        )

        // Top overlay - joke text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(OverlayBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (jokeSetup.isNotEmpty()) {
                Text(
                    text = jokeSetup,
                    color = JokeSetup,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (jokePunchline.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = jokePunchline,
                    color = JokePunchline,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom overlay - controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(OverlayBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status text
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (faceStatusText.isNotEmpty() && supportsFaceDetection()) {
                Text(
                    text = faceStatusText,
                    color = DetectionSmile,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66FFFFFF))
                ) {
                    Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // Capture button (only on camera platforms)
                if (supportsCameraCapture()) {
                    IconButton(
                        onClick = { captureController.capture() },
                        enabled = !isAutoMode,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isAutoMode) Color.Gray else Color.White)
                        )
                    }
                }

                // Switch camera button (only on camera platforms)
                if (supportsCameraCapture()) {
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x66FFFFFF))
                    ) {
                        Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Manual joke button
                if (manualMode || !supportsCameraCapture()) {
                    Button(
                        onClick = { tellJoke() },
                        enabled = !isAutoMode && ttsReady,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Tell a Joke")
                    }
                }

                // Start/Stop auto mode
                Button(
                    onClick = {
                        isAutoMode = !isAutoMode
                        if (isAutoMode) {
                            statusText = "Auto mode - Starting..."
                            scope.launch {
                                delay(1000)
                                tellJoke()
                            }
                        } else {
                            isWaitingForReaction = false
                            ttsEngine.stop()
                            statusText = "Stopped"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoMode) Accent else Primary
                    )
                ) {
                    Text(if (isAutoMode) "Stop" else "Start Auto")
                }
            }
        }
    }
}
