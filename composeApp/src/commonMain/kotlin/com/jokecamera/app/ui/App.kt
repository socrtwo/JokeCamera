package com.jokecamera.app.ui

import androidx.compose.runtime.*
import com.jokecamera.app.data.JokeRepository
import com.jokecamera.app.platform.SettingsStore
import com.jokecamera.app.platform.TtsEngine
import com.jokecamera.app.platform.createSettingsStore
import com.jokecamera.app.platform.createTtsEngine
import com.jokecamera.app.ui.theme.JokeCameraTheme

enum class Screen { MAIN, SETTINGS }

@Composable
fun App() {
    val settingsStore = remember { createSettingsStore() }
    val jokeRepository = remember { JokeRepository(settingsStore) }
    val ttsEngine = remember { createTtsEngine() }

    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    DisposableEffect(ttsEngine) {
        ttsEngine.initialize(
            onReady = { /* TTS ready */ },
            onError = { /* TTS error */ }
        )
        onDispose {
            ttsEngine.shutdown()
        }
    }

    JokeCameraTheme {
        when (currentScreen) {
            Screen.MAIN -> MainScreen(
                jokeRepository = jokeRepository,
                settingsStore = settingsStore,
                ttsEngine = ttsEngine,
                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
            )
            Screen.SETTINGS -> SettingsScreen(
                jokeRepository = jokeRepository,
                settingsStore = settingsStore,
                onNavigateBack = { currentScreen = Screen.MAIN }
            )
        }
    }
}
