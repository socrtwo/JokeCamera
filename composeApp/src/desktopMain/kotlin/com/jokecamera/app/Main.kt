package com.jokecamera.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jokecamera.app.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Joke Camera",
        state = rememberWindowState(width = 400.dp, height = 700.dp)
    ) {
        App()
    }
}
