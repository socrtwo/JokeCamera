# Joke Camera

A fun Android app that tells jokes and automatically captures photos when it detects smiles or laughter!

## Overview

Joke Camera combines text-to-speech joke telling with real-time face detection to capture genuine reactions. The app tells a joke, waits for your smile or laugh, and automatically snaps a photo at just the right moment.

## Features

### Core Features
- **451 Classic Jokes**: Curated collection of quality jokes organized by category
- **Text-to-Speech**: Jokes are spoken aloud using Android's TTS engine
- **Face Detection**: Uses Google ML Kit to detect faces and analyze expressions
- **Smile/Laugh Detection**: Automatically detects when subjects smile or laugh
- **Automatic Photo Capture**: Takes a photo when a smile or laugh is detected
- **No Repeat Jokes**: Tracks which jokes have been told and won't repeat any until all have been used

### Camera Features
- **Front & Back Camera Support**: Switch between cameras with one tap
- **Permission Handling**: Properly requests camera and storage permissions
- **Photo Gallery Saving**: Photos are saved to Pictures/JokeCamera folder

### Joke Categories

Version 2 organizes all 451 Classic jokes into four categories:

| Category | Description |
|----------|-------------|
| **General** | Classic humor for all audiences |
| **Programming** | Tech and coding jokes for developers |
| **Knock-Knock** | Traditional call-and-response jokes |
| **Dad** | Family-friendly groan-worthy puns |

## Configuration Options (Settings Screen)

### Joke Category Filtering
- Enable or disable each joke category via checkboxes
- At least one category must always be selected
- Category labels show the number of jokes available in each

### Joke Management
- **Download Template**: Get a sample JSON file showing the proper format for custom jokes
- **Export Jokes**: Save your current joke collection to a JSON file
- **Upload Jokes**: Import custom jokes from a JSON file
- **Replace or Add**: Choose to replace built-in jokes entirely or add custom jokes to the collection
- **Custom Categories**: Create your own joke categories when importing

### Manual Joke Mode
- When enabled, shows a "Tell a Joke" button in the main interface
- Allows telling one joke at a time manually instead of automatic mode

### Smile/Laugh Detection Toggle
- Enable or disable automatic face detection
- When disabled, photos must be taken manually

### Timer Mode
- Take photos after a configurable delay instead of waiting for smile/laugh detection
- Adjustable timer from 0.5 to 10.0 seconds

### Detection Mode
Choose how the app triggers photo capture:
- **Smile Only**: Trigger on any smile
- **Laugh Only**: Trigger only on big smiles (laughs)
- **Smile AND Laugh**: Requires detecting a smile first, then a bigger laugh
- **Smile OR Laugh** (Default): Triggers on either a smile or laugh

### Timing Settings
- **Wait Time Before Next Joke**: How long to wait for a reaction before telling another joke (0.5-10 seconds, default: 2.5)
- **Setup to Punchline Delay**: Pause between the setup and punchline (0-2 seconds, default: 0.81)

### Joke Statistics
- Shows how many jokes have been told out of the total
- Displays counts per category and overall
- Shows custom jokes count separately
- Reset button to mark all jokes as untold

## How It Works

### Automatic Mode
1. Press "Start Auto" to begin automatic joke telling
2. The app tells a joke (setup, pause, punchline)
3. After the joke, it waits for a smile or laugh
4. When detected, it automatically takes a photo
5. If no reaction is detected within the configured wait time, it tells another joke
6. This continues until you press "Stop"

### Manual Mode
1. Enable "Manual Joke Mode" in settings
2. Press the "Tell a Joke" button to hear one joke
3. The app waits for a smile/laugh or you can capture manually

## Custom Jokes

### JSON Format
Custom jokes use a simple JSON array format:

```json
[
  {
    "type": "general",
    "setup": "Why did the chicken cross the road?",
    "punchline": "To get to the other side!"
  },
  {
    "type": "programming",
    "setup": "Why do programmers prefer dark mode?",
    "punchline": "Because light attracts bugs!"
  },
  {
    "type": "dad",
    "setup": "I'm reading a book about anti-gravity.",
    "punchline": "It's impossible to put down!"
  },
  {
    "type": "knock-knock",
    "setup": "Knock knock.\nWho's there?\nBoo.\nBoo who?",
    "punchline": "Don't cry, it's just a joke!"
  }
]
```

### Custom Categories
You can create your own categories by using any string for the "type" field. Custom categories will appear in the category filter list.

## Technical Details

### Dependencies
- **CameraX**: For camera preview and image capture
- **ML Kit Face Detection**: For real-time face and expression analysis
- **Text-to-Speech**: Android's built-in TTS for speaking jokes
- **Material Design Components**: Modern UI elements

### Permissions Required
- `CAMERA`: For camera preview and photo capture
- `WRITE_EXTERNAL_STORAGE` (Android 9 and below): For saving photos
- `READ_MEDIA_IMAGES` (Android 13+): For accessing saved photos
- `MODIFY_AUDIO_SETTINGS`: For TTS volume control

### Minimum Requirements
- Android 7.0 (API 24) or higher
- Camera (front or back)
- Speaker or audio output

## Tips for Best Results

1. **Lighting**: Good lighting helps face detection work better
2. **Distance**: Keep subjects 2-6 feet from the camera
3. **Angle**: Face the camera directly for best detection
4. **Multiple Faces**: The app can detect multiple faces simultaneously
5. **Timing**: Adjust the punchline delay based on joke delivery preference

## Installation

Download the latest APK from the [Releases](https://github.com/socrtwo/JokeCamera/releases) page.

## License

This project is provided for personal and educational use.

## Version History

- **2.0.0**: Major update
  - Replaced 1000 jokes with 451 curated Classic jokes
  - Added 4 joke categories (General, Programming, Knock-Knock, Dad)
  - Added joke category filtering with checkboxes
  - Added joke management system (import/export via JSON)
  - Added custom joke support with user-defined categories
- **1.0.0**: Initial release with 1000 jokes and full configuration options
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build Steps
1. Open the project in Android Studio.
2. Sync Gradle files.
3. Build > Make Project.
4. Run on a device or emulator with camera support.

### APK Generation
```bash
./gradlew assembleDebug
```
The APK will be in `app/build/outputs/apk/debug/`

## Project Structure

```
JokeCamera/
├── app/
│   ├── src/main/
│   │   ├── java/com/jokecamera/app/
│   │   │   ├── MainActivity.kt         # Main camera and joke UI
│   │   │   ├── SettingsActivity.kt     # Configuration screen
│   │   │   ├── JokeManager.kt          # 1000 jokes + tracking
│   │   │   └── FaceAnalyzer.kt         # ML Kit face detection
│   │   ├── res/
│   │   │   ├── layout/                 # XML layouts
│   │   │   ├── values/                 # Strings, colors, themes
│   │   │   ├── drawable/               # Icons and backgrounds
│   │   │   └── xml/                    # File provider paths
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Joke Categories

The 1000 jokes are organized into these categories:
- Classic One-Liners (1-50)
- Food and Restaurant (51-150)
- Animals (101-250)
- Work and Office (251-350)
- Science and Tech (351-450)
- Sports and Games (451-550)
- Family and Kids (551-650)
- Travel and Places (651-750)
- Music and Entertainment (751-850)
- Weather and Seasons (851-900)
- Miscellaneous (901-1000)

## Tips for Best Results

1. **Lighting**: Good lighting helps face detection work better.
2. **Distance**: Keep subjects 2-6 feet from the camera.
3. **Angle**: Face the camera directly for best detection.
4. **Multiple Faces**: The app can detect multiple faces simultaneously.
5. **Timing**: Adjust the punchline delay based on joke delivery preference.

## License

This project is provided for personal and educational use.

## Version History

- **1.0**: Initial release with 1000 jokes and full configuration options.
