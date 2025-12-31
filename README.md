# Joke Camera App 📸😂

An Android app that tells jokes and captures photos when people smile or laugh!

## Features

### Core Functionality
- **1000 Classic Jokes**: A curated collection of classic jokes covering various categories including animals, food, work, science, sports, family, travel, music, weather, and more.
- **No Repeat Jokes**: The app tracks which jokes have been told and won't repeat any until all 1000 have been used.
- **Text-to-Speech**: Jokes are spoken aloud using Android's TTS engine.
- **Face Detection**: Uses Google ML Kit to detect faces and analyze expressions.
- **Smile/Laugh Detection**: Automatically detects when subjects smile or laugh.
- **Automatic Photo Capture**: Takes a photo when a smile or laugh is detected.

### Camera Features
- **Front & Back Camera Support**: Switch between cameras with one tap.
- **Permission Handling**: Properly requests camera and storage permissions.
- **Photo Gallery Saving**: Photos are saved to Pictures/JokeCamera folder.

### Configuration Options (Settings Screen)

#### 1. Manual Joke Mode
- When enabled, shows a "Tell a Joke" button in the main interface.
- Allows telling one joke at a time manually instead of automatic mode.

#### 2. Smile/Laugh Detection Toggle
- Enable or disable automatic face detection.
- When disabled, photos must be taken manually.

#### 3. Timer Mode
- Take photos after a configurable delay instead of waiting for smile/laugh detection.
- Adjustable timer from 0.5 to 10.0 seconds.

#### 4. Detection Mode (Radio Buttons)
- **Smile Only**: Trigger on any smile.
- **Laugh Only**: Trigger only on big smiles (laughs).
- **Smile AND Laugh**: Requires detecting a smile first, then a bigger laugh.
- **Smile OR Laugh** (Default): Triggers on either a smile or laugh.

#### 5. Wait Time Before Next Joke
- Configurable time to wait for a reaction before telling another joke.
- Range: 0.5 to 10.0 seconds.
- Default: 2.5 seconds.

#### 6. Setup to Punchline Delay
- Time between the setup line and punchline.
- Range: 0.00 to 2.00 seconds.
- Default: 0.81 seconds.

#### 7. Joke Statistics
- Shows how many jokes have been told out of 1000.
- Reset button to mark all jokes as untold.

## How It Works

### Automatic Mode
1. Press "Start Auto" to begin automatic joke telling.
2. The app tells a joke (setup, pause, punchline).
3. After the joke, it waits for a smile or laugh.
4. When detected, it automatically takes a photo.
5. If no reaction is detected within the configured wait time, it tells another joke.
6. This continues until you press "Stop".

### Manual Mode
1. Enable "Manual Joke Mode" in settings.
2. Press the "Tell a Joke" button to hear one joke.
3. The app waits for a smile/laugh or you can capture manually.

## Technical Details

### Dependencies
- **CameraX**: For camera preview and image capture.
- **ML Kit Face Detection**: For real-time face and expression analysis.
- **Text-to-Speech**: Android's built-in TTS for speaking jokes.
- **Material Design Components**: Modern UI elements.

### Permissions Required
- `CAMERA`: For camera preview and photo capture.
- `WRITE_EXTERNAL_STORAGE` (Android 9 and below): For saving photos.
- `READ_MEDIA_IMAGES` (Android 13+): For accessing saved photos.
- `MODIFY_AUDIO_SETTINGS`: For TTS volume control.

### Detection Thresholds
- **Smile**: Detected when smiling probability >= 40%
- **Laugh**: Detected when smiling probability >= 75%

## Building the App

### Prerequisites
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
