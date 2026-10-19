package com.jokecamera.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1976D2)
val PrimaryDark = Color(0xFF1565C0)
val Accent = Color(0xFFFF5722)
val JokeSetup = Color.White
val JokePunchline = Color(0xFFFFD700)
val DetectionSmile = Color(0xFF4CAF50)
val DetectionLaugh = Color(0xFFFF9800)
val OverlayBackground = Color(0xCC000000)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Accent,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Accent,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121)
)

@Composable
fun JokeCameraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
