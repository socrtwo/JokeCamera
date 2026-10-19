package com.jokecamera.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jokecamera.app.data.JokeRepository
import com.jokecamera.app.model.AppSettings
import com.jokecamera.app.model.DetectionMode
import com.jokecamera.app.model.JokeType
import com.jokecamera.app.platform.SettingsStore
import com.jokecamera.app.platform.supportsFaceDetection

@Composable
fun SettingsScreen(
    jokeRepository: JokeRepository,
    settingsStore: SettingsStore,
    onNavigateBack: () -> Unit
) {
    var manualJoke by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_MANUAL_JOKE, AppSettings.DEFAULT_MANUAL_JOKE)) }
    var detectionEnabled by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_DETECTION_ENABLED, AppSettings.DEFAULT_DETECTION_ENABLED)) }
    var timerMode by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_TIMER_MODE, AppSettings.DEFAULT_TIMER_MODE)) }
    var timerDelay by remember { mutableFloatStateOf(settingsStore.getFloat(AppSettings.KEY_TIMER_DELAY, AppSettings.DEFAULT_TIMER_DELAY)) }
    var detectionModeInt by remember { mutableIntStateOf(settingsStore.getInt(AppSettings.KEY_DETECTION_MODE, AppSettings.DEFAULT_DETECTION_MODE)) }
    var nextJokeWait by remember { mutableFloatStateOf(settingsStore.getFloat(AppSettings.KEY_NEXT_JOKE_WAIT, AppSettings.DEFAULT_NEXT_JOKE_WAIT)) }
    var punchlineDelay by remember { mutableFloatStateOf(settingsStore.getFloat(AppSettings.KEY_PUNCHLINE_DELAY, AppSettings.DEFAULT_PUNCHLINE_DELAY)) }

    // Categories
    var catGeneral by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)) }
    var catProgramming by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_CATEGORY_PROGRAMMING, true)) }
    var catKnockKnock by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_CATEGORY_KNOCK_KNOCK, true)) }
    var catDad by remember { mutableStateOf(settingsStore.getBoolean(AppSettings.KEY_CATEGORY_DAD, true)) }

    // Stats
    var remaining by remember { mutableIntStateOf(jokeRepository.getRemainingCount()) }
    var total by remember { mutableIntStateOf(jokeRepository.getTotalCount()) }

    fun refreshStats() {
        remaining = jokeRepository.getRemainingCount()
        total = jokeRepository.getTotalCount()
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Joke Mode Section
            SectionHeader("JOKE TELLING MODE")
            SettingSwitch(
                title = "Manual Joke Mode",
                description = "Show a button to manually trigger jokes",
                checked = manualJoke,
                onCheckedChange = {
                    manualJoke = it
                    settingsStore.putBoolean(AppSettings.KEY_MANUAL_JOKE, it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Joke Categories Section
            SectionHeader("JOKE CATEGORIES")
            Text(
                "Select which types of jokes to include",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CategoryCheckbox("General (${jokeRepository.getCountByType(JokeType.GENERAL)} jokes)", catGeneral) {
                catGeneral = it
                settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, it)
                if (!catGeneral && !catProgramming && !catKnockKnock && !catDad) {
                    catGeneral = true
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)
                }
                refreshStats()
            }
            CategoryCheckbox("Programming (${jokeRepository.getCountByType(JokeType.PROGRAMMING)} jokes)", catProgramming) {
                catProgramming = it
                settingsStore.putBoolean(AppSettings.KEY_CATEGORY_PROGRAMMING, it)
                if (!catGeneral && !catProgramming && !catKnockKnock && !catDad) {
                    catGeneral = true
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)
                }
                refreshStats()
            }
            CategoryCheckbox("Knock-Knock (${jokeRepository.getCountByType(JokeType.KNOCK_KNOCK)} jokes)", catKnockKnock) {
                catKnockKnock = it
                settingsStore.putBoolean(AppSettings.KEY_CATEGORY_KNOCK_KNOCK, it)
                if (!catGeneral && !catProgramming && !catKnockKnock && !catDad) {
                    catGeneral = true
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)
                }
                refreshStats()
            }
            CategoryCheckbox("Dad Jokes (${jokeRepository.getCountByType(JokeType.DAD)} jokes)", catDad) {
                catDad = it
                settingsStore.putBoolean(AppSettings.KEY_CATEGORY_DAD, it)
                if (!catGeneral && !catProgramming && !catKnockKnock && !catDad) {
                    catGeneral = true
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)
                }
                refreshStats()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = {
                    catGeneral = true; catProgramming = true; catKnockKnock = true; catDad = true
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_PROGRAMMING, true)
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_KNOCK_KNOCK, true)
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_DAD, true)
                    refreshStats()
                }) { Text("Select All") }

                OutlinedButton(onClick = {
                    catGeneral = true; catProgramming = false; catKnockKnock = false; catDad = false
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_PROGRAMMING, false)
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_KNOCK_KNOCK, false)
                    settingsStore.putBoolean(AppSettings.KEY_CATEGORY_DAD, false)
                    refreshStats()
                }) { Text("Select None") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Capture Mode Section
            if (supportsFaceDetection()) {
                SectionHeader("PHOTO CAPTURE MODE")
                SettingSwitch(
                    title = "Smile/Laugh Detection",
                    description = "Automatically detect smiles and laughs to trigger photo capture",
                    checked = detectionEnabled,
                    onCheckedChange = {
                        detectionEnabled = it
                        settingsStore.putBoolean(AppSettings.KEY_DETECTION_ENABLED, it)
                    }
                )

                SettingSwitch(
                    title = "Timer Mode",
                    description = "Take photo after a set time instead of waiting for smile/laugh",
                    checked = timerMode,
                    onCheckedChange = {
                        timerMode = it
                        settingsStore.putBoolean(AppSettings.KEY_TIMER_MODE, it)
                    }
                )

                if (timerMode) {
                    SettingSlider(
                        title = "Timer Delay",
                        value = timerDelay,
                        valueRange = AppSettings.MIN_TIMER_DELAY..AppSettings.MAX_TIMER_DELAY,
                        steps = 19,
                        format = "%.1f seconds",
                        onValueChange = {
                            timerDelay = it
                            settingsStore.putFloat(AppSettings.KEY_TIMER_DELAY, it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detection Mode Section
                if (detectionEnabled) {
                    SectionHeader("DETECTION MODE")
                    DetectionMode.entries.forEachIndexed { index, mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = detectionModeInt == index,
                                onClick = {
                                    detectionModeInt = index
                                    settingsStore.putInt(AppSettings.KEY_DETECTION_MODE, index)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.displayName(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Timing Section
            SectionHeader("TIMING SETTINGS")
            SettingSlider(
                title = "Setup to Punchline Delay",
                value = punchlineDelay,
                valueRange = AppSettings.MIN_PUNCHLINE_DELAY..AppSettings.MAX_PUNCHLINE_DELAY,
                steps = 200,
                format = "%.2f seconds",
                onValueChange = {
                    punchlineDelay = it
                    settingsStore.putFloat(AppSettings.KEY_PUNCHLINE_DELAY, it)
                }
            )

            SettingSlider(
                title = "Wait Before Next Joke",
                value = nextJokeWait,
                valueRange = AppSettings.MIN_NEXT_JOKE_WAIT..AppSettings.MAX_NEXT_JOKE_WAIT,
                steps = 19,
                format = "%.1f seconds",
                onValueChange = {
                    nextJokeWait = it
                    settingsStore.putFloat(AppSettings.KEY_NEXT_JOKE_WAIT, it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Statistics Section
            SectionHeader("JOKE STATISTICS")
            val told = total - remaining
            Text(
                "Jokes told: $told / $total ($remaining remaining)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Total in all categories: ${jokeRepository.getAllJokesCount()}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    jokeRepository.resetToldJokes()
                    refreshStats()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reset All Jokes")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Jokes Section
            SectionHeader("JOKE MANAGEMENT")
            val customCount = jokeRepository.getCustomCount()
            val builtInCount = jokeRepository.getBuiltInCount()
            val isReplaced = jokeRepository.isBuiltInReplaced()
            Text(
                when {
                    isReplaced -> "Custom jokes: $customCount (built-in jokes replaced)"
                    customCount > 0 -> "Custom jokes: $customCount (added to $builtInCount built-in)"
                    else -> "Using $builtInCount built-in jokes"
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (customCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    jokeRepository.clearCustomJokes()
                    refreshStats()
                }) {
                    Text("Clear Custom Jokes")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(format.format(value), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun CategoryCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
