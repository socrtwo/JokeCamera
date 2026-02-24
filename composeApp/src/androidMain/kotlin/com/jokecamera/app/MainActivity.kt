package com.jokecamera.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jokecamera.app.platform.initAndroidContext
import com.jokecamera.app.platform.initTtsContext
import com.jokecamera.app.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize platform context for settings and TTS
        initAndroidContext(this)
        initTtsContext(this)

        setContent {
            App()
        }
    }
}
