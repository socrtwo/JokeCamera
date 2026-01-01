package com.jokecamera.app

import android.os.Bundle
import android.os.Environment
import android.widget.SeekBar
import android.widget.CheckBox
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Button
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Settings/Configuration Activity for JokeCamera app.
 * Allows users to customize:
 * - Manual vs automatic joke telling
 * - Smile/laugh detection on/off
 * - Timer-based photo capture
 * - Detection mode (smile only, laugh only, both, either)
 * - Wait time before next joke
 * - Timing between setup and punchline (0.00-2.00 seconds)
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    
    // UI Elements
    private lateinit var switchManualJoke: SwitchCompat
    private lateinit var switchDetectionEnabled: SwitchCompat
    private lateinit var switchTimerMode: SwitchCompat
    private lateinit var sliderTimerDelay: Slider
    private lateinit var textTimerValue: TextView
    private lateinit var radioGroupDetection: RadioGroup
    private lateinit var sliderNextJokeWait: Slider
    private lateinit var textNextJokeValue: TextView
    private lateinit var sliderPunchlineDelay: Slider
    private lateinit var textPunchlineValue: TextView
    private lateinit var buttonResetJokes: Button
    private lateinit var textJokeStats: TextView
    
    // Category checkboxes
    private lateinit var checkboxGeneral: CheckBox
    private lateinit var checkboxProgramming: CheckBox
    private lateinit var checkboxKnockKnock: CheckBox
    private lateinit var checkboxDad: CheckBox
    private lateinit var buttonSelectAll: Button
    private lateinit var buttonSelectNone: Button
    
    // Joke management
    private lateinit var buttonDownloadTemplate: Button
    private lateinit var buttonDownloadJokes: Button
    private lateinit var buttonUploadJokes: Button
    private lateinit var checkboxReplaceJokes: CheckBox
    private lateinit var buttonClearCustom: Button
    private lateinit var textCustomJokeStats: TextView
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFileSelected(it) }
    }
    
    companion object {
        // Preference keys
        const val KEY_MANUAL_JOKE = "manual_joke_mode"
        const val KEY_DETECTION_ENABLED = "detection_enabled"
        const val KEY_TIMER_MODE = "timer_mode"
        const val KEY_TIMER_DELAY = "timer_delay"
        const val KEY_DETECTION_MODE = "detection_mode"
        const val KEY_NEXT_JOKE_WAIT = "next_joke_wait"
        const val KEY_PUNCHLINE_DELAY = "punchline_delay"
        
        // Default values
        const val DEFAULT_MANUAL_JOKE = false
        const val DEFAULT_DETECTION_ENABLED = true
        const val DEFAULT_TIMER_MODE = false
        const val DEFAULT_TIMER_DELAY = 3.0f      // seconds after joke for timer photo
        const val DEFAULT_DETECTION_MODE = 3      // 0=smile, 1=laugh, 2=both, 3=either
        const val DEFAULT_NEXT_JOKE_WAIT = 2.5f   // seconds to wait before next joke
        const val DEFAULT_PUNCHLINE_DELAY = 0.81f // seconds between setup and punchline
        
        // Range limits
        const val MIN_PUNCHLINE_DELAY = 0.0f
        const val MAX_PUNCHLINE_DELAY = 2.0f
        const val MIN_NEXT_JOKE_WAIT = 0.5f
        const val MAX_NEXT_JOKE_WAIT = 10.0f
        const val MIN_TIMER_DELAY = 0.5f
        const val MAX_TIMER_DELAY = 10.0f
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        initializeViews()
        loadSettings()
        setupListeners()
        updateJokeStats()
        updateCustomJokeStats()
    }
    
    private fun initializeViews() {
        switchManualJoke = findViewById(R.id.switch_manual_joke)
        switchDetectionEnabled = findViewById(R.id.switch_detection_enabled)
        switchTimerMode = findViewById(R.id.switch_timer_mode)
        sliderTimerDelay = findViewById(R.id.slider_timer_delay)
        textTimerValue = findViewById(R.id.text_timer_value)
        radioGroupDetection = findViewById(R.id.radio_group_detection)
        sliderNextJokeWait = findViewById(R.id.slider_next_joke_wait)
        textNextJokeValue = findViewById(R.id.text_next_joke_value)
        sliderPunchlineDelay = findViewById(R.id.slider_punchline_delay)
        textPunchlineValue = findViewById(R.id.text_punchline_value)
        buttonResetJokes = findViewById(R.id.button_reset_jokes)
        textJokeStats = findViewById(R.id.text_joke_stats)
        
        // Category checkboxes
        checkboxGeneral = findViewById(R.id.checkbox_general)
        checkboxProgramming = findViewById(R.id.checkbox_programming)
        checkboxKnockKnock = findViewById(R.id.checkbox_knock_knock)
        checkboxDad = findViewById(R.id.checkbox_dad)
        buttonSelectAll = findViewById(R.id.button_select_all)
        buttonSelectNone = findViewById(R.id.button_select_none)
        
        // Joke management
        buttonDownloadTemplate = findViewById(R.id.button_download_template)
        buttonDownloadJokes = findViewById(R.id.button_download_jokes)
        buttonUploadJokes = findViewById(R.id.button_upload_jokes)
        checkboxReplaceJokes = findViewById(R.id.checkbox_replace_jokes)
        buttonClearCustom = findViewById(R.id.button_clear_custom)
        textCustomJokeStats = findViewById(R.id.text_custom_joke_stats)
        
        // Configure slider ranges
        sliderPunchlineDelay.valueFrom = MIN_PUNCHLINE_DELAY
        sliderPunchlineDelay.valueTo = MAX_PUNCHLINE_DELAY
        sliderPunchlineDelay.stepSize = 0.01f
        
        sliderNextJokeWait.valueFrom = MIN_NEXT_JOKE_WAIT
        sliderNextJokeWait.valueTo = MAX_NEXT_JOKE_WAIT
        sliderNextJokeWait.stepSize = 0.1f
        
        sliderTimerDelay.valueFrom = MIN_TIMER_DELAY
        sliderTimerDelay.valueTo = MAX_TIMER_DELAY
        sliderTimerDelay.stepSize = 0.1f
    }
    
    private fun loadSettings() {
        // Load all settings from preferences
        switchManualJoke.isChecked = prefs.getBoolean(KEY_MANUAL_JOKE, DEFAULT_MANUAL_JOKE)
        switchDetectionEnabled.isChecked = prefs.getBoolean(KEY_DETECTION_ENABLED, DEFAULT_DETECTION_ENABLED)
        switchTimerMode.isChecked = prefs.getBoolean(KEY_TIMER_MODE, DEFAULT_TIMER_MODE)
        
        val timerDelay = prefs.getFloat(KEY_TIMER_DELAY, DEFAULT_TIMER_DELAY)
        sliderTimerDelay.value = timerDelay
        textTimerValue.text = String.format("%.1f seconds", timerDelay)
        
        val detectionMode = prefs.getInt(KEY_DETECTION_MODE, DEFAULT_DETECTION_MODE)
        when (detectionMode) {
            0 -> radioGroupDetection.check(R.id.radio_smile_only)
            1 -> radioGroupDetection.check(R.id.radio_laugh_only)
            2 -> radioGroupDetection.check(R.id.radio_smile_and_laugh)
            3 -> radioGroupDetection.check(R.id.radio_smile_or_laugh)
        }
        
        val nextJokeWait = prefs.getFloat(KEY_NEXT_JOKE_WAIT, DEFAULT_NEXT_JOKE_WAIT)
        sliderNextJokeWait.value = nextJokeWait
        textNextJokeValue.text = String.format("%.1f seconds", nextJokeWait)
        
        val punchlineDelay = prefs.getFloat(KEY_PUNCHLINE_DELAY, DEFAULT_PUNCHLINE_DELAY)
        sliderPunchlineDelay.value = punchlineDelay
        textPunchlineValue.text = String.format("%.2f seconds", punchlineDelay)
        
        // Load category settings
        checkboxGeneral.isChecked = prefs.getBoolean(JokeManager.KEY_CATEGORY_GENERAL, true)
        checkboxProgramming.isChecked = prefs.getBoolean(JokeManager.KEY_CATEGORY_PROGRAMMING, true)
        checkboxKnockKnock.isChecked = prefs.getBoolean(JokeManager.KEY_CATEGORY_KNOCK_KNOCK, true)
        checkboxDad.isChecked = prefs.getBoolean(JokeManager.KEY_CATEGORY_DAD, true)
        
        // Update checkbox labels with joke counts
        updateCategoryLabels()
        
        // Update UI state based on settings
        updateTimerModeVisibility()
        updateDetectionModeVisibility()
    }
    
    private fun setupListeners() {
        // Manual joke mode switch
        switchManualJoke.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_MANUAL_JOKE, isChecked).apply()
        }
        
        // Detection enabled switch
        switchDetectionEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DETECTION_ENABLED, isChecked).apply()
            updateDetectionModeVisibility()
        }
        
        // Timer mode switch
        switchTimerMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_TIMER_MODE, isChecked).apply()
            updateTimerModeVisibility()
        }
        
        // Timer delay slider
        sliderTimerDelay.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat(KEY_TIMER_DELAY, value).apply()
            textTimerValue.text = String.format("%.1f seconds", value)
        }
        
        // Detection mode radio group
        radioGroupDetection.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_smile_only -> 0
                R.id.radio_laugh_only -> 1
                R.id.radio_smile_and_laugh -> 2
                R.id.radio_smile_or_laugh -> 3
                else -> 3
            }
            prefs.edit().putInt(KEY_DETECTION_MODE, mode).apply()
        }
        
        // Next joke wait slider
        sliderNextJokeWait.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat(KEY_NEXT_JOKE_WAIT, value).apply()
            textNextJokeValue.text = String.format("%.1f seconds", value)
        }
        
        // Punchline delay slider
        sliderPunchlineDelay.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat(KEY_PUNCHLINE_DELAY, value).apply()
            textPunchlineValue.text = String.format("%.2f seconds", value)
        }
        
        // Reset jokes button
        buttonResetJokes.setOnClickListener {
            val jokeManager = JokeManager(this)
            jokeManager.resetToldJokes()
            updateJokeStats()
        }
        
        // Category checkbox listeners
        checkboxGeneral.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(JokeManager.KEY_CATEGORY_GENERAL, isChecked).apply()
            ensureAtLeastOneCategory()
            updateJokeStats()
        }
        
        checkboxProgramming.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(JokeManager.KEY_CATEGORY_PROGRAMMING, isChecked).apply()
            ensureAtLeastOneCategory()
            updateJokeStats()
        }
        
        checkboxKnockKnock.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(JokeManager.KEY_CATEGORY_KNOCK_KNOCK, isChecked).apply()
            ensureAtLeastOneCategory()
            updateJokeStats()
        }
        
        checkboxDad.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(JokeManager.KEY_CATEGORY_DAD, isChecked).apply()
            ensureAtLeastOneCategory()
            updateJokeStats()
        }
        
        // Select All button
        buttonSelectAll.setOnClickListener {
            checkboxGeneral.isChecked = true
            checkboxProgramming.isChecked = true
            checkboxKnockKnock.isChecked = true
            checkboxDad.isChecked = true
        }
        
        // Select None button (will auto-revert to General if all unchecked)
        buttonSelectNone.setOnClickListener {
            checkboxGeneral.isChecked = false
            checkboxProgramming.isChecked = false
            checkboxKnockKnock.isChecked = false
            checkboxDad.isChecked = false
        }
        
        // Joke management listeners
        buttonDownloadTemplate.setOnClickListener {
            downloadTemplate()
        }
        
        buttonDownloadJokes.setOnClickListener {
            downloadCurrentJokes()
        }
        
        buttonUploadJokes.setOnClickListener {
            filePickerLauncher.launch("application/json")
        }
        
        buttonClearCustom.setOnClickListener {
            val jokeManager = JokeManager(this)
            jokeManager.clearCustomJokes()
            updateJokeStats()
            updateCustomJokeStats()
            Toast.makeText(this, getString(R.string.custom_jokes_cleared), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateTimerModeVisibility() {
        val timerEnabled = switchTimerMode.isChecked
        sliderTimerDelay.isEnabled = timerEnabled
        textTimerValue.alpha = if (timerEnabled) 1.0f else 0.5f
    }
    
    private fun updateDetectionModeVisibility() {
        val detectionEnabled = switchDetectionEnabled.isChecked
        for (i in 0 until radioGroupDetection.childCount) {
            radioGroupDetection.getChildAt(i).isEnabled = detectionEnabled
        }
        radioGroupDetection.alpha = if (detectionEnabled) 1.0f else 0.5f
    }
    
    private fun updateJokeStats() {
        val jokeManager = JokeManager(this)
        val remaining = jokeManager.getRemainingCount()
        val total = jokeManager.getTotalCount()
        val told = total - remaining
        val allJokes = jokeManager.getAllJokesCount()
        textJokeStats.text = "Jokes told: $told / $total ($remaining remaining)\nTotal in all categories: $allJokes"
    }
    
    private fun updateCategoryLabels() {
        val jokeManager = JokeManager(this)
        checkboxGeneral.text = String.format("General (%d jokes)", jokeManager.getCountByType(JokeType.GENERAL))
        checkboxProgramming.text = String.format("Programming (%d jokes)", jokeManager.getCountByType(JokeType.PROGRAMMING))
        checkboxKnockKnock.text = String.format("Knock-Knock (%d jokes)", jokeManager.getCountByType(JokeType.KNOCK_KNOCK))
        checkboxDad.text = String.format("Dad Jokes (%d jokes)", jokeManager.getCountByType(JokeType.DAD))
    }
    
    private fun ensureAtLeastOneCategory() {
        // If all categories are unchecked, auto-check General
        if (!checkboxGeneral.isChecked && !checkboxProgramming.isChecked && 
            !checkboxKnockKnock.isChecked && !checkboxDad.isChecked) {
            checkboxGeneral.isChecked = true
            android.widget.Toast.makeText(this, "At least one category must be selected", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateCustomJokeStats() {
        val jokeManager = JokeManager(this)
        val customCount = jokeManager.getCustomCount()
        val builtInCount = jokeManager.getBuiltInCount()
        val isReplaced = jokeManager.isBuiltInReplaced()
        
        val statusText = if (isReplaced) {
            "Custom jokes: $customCount (built-in jokes replaced)"
        } else if (customCount > 0) {
            "Custom jokes: $customCount (added to $builtInCount built-in)"
        } else {
            "Custom jokes: 0 (using $builtInCount built-in jokes)"
        }
        textCustomJokeStats.text = statusText
    }
    
    private fun downloadTemplate() {
        try {
            val jokeManager = JokeManager(this)
            val template = jokeManager.getTemplateJson()
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "joke_template.json")
            
            FileOutputStream(file).use { fos ->
                fos.write(template.toByteArray())
            }
            
            Toast.makeText(this, getString(R.string.template_downloaded), Toast.LENGTH_LONG).show()
            
            // Notify media scanner
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            sendBroadcast(intent)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun downloadCurrentJokes() {
        try {
            val jokeManager = JokeManager(this)
            val jokesJson = jokeManager.exportJokesAsJson()
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val timestamp = System.currentTimeMillis()
            val file = File(downloadsDir, "jokes_export_$timestamp.json")
            
            FileOutputStream(file).use { fos ->
                fos.write(jokesJson.toByteArray())
            }
            
            Toast.makeText(this, getString(R.string.jokes_downloaded), Toast.LENGTH_LONG).show()
            
            // Notify media scanner
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            sendBroadcast(intent)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleFileSelected(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            
            val jokeManager = JokeManager(this)
            val replaceExisting = checkboxReplaceJokes.isChecked
            val count = jokeManager.importJokes(jsonString, replaceExisting)
            
            Toast.makeText(this, String.format(getString(R.string.jokes_imported), count), Toast.LENGTH_LONG).show()
            
            updateJokeStats()
            updateCustomJokeStats()
            updateCategoryLabels()
            
        } catch (e: Exception) {
            Toast.makeText(this, String.format(getString(R.string.jokes_import_failed), e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
