# Joke Camera

A fun multi-platform app that tells jokes and automatically captures photos when it detects smiles or laughter!

Available for **Android**, **iOS**, **Windows**, **macOS**, **Linux**, and **Web**.

## Overview

Joke Camera combines text-to-speech joke telling with real-time face detection to capture genuine reactions. The app tells a joke, waits for your smile or laugh, and automatically snaps a photo at just the right moment.

## Features

### Core Features
- **451 Classic Jokes**: Curated collection of quality jokes organized by category
- **Text-to-Speech**: Jokes are spoken aloud
- **Face Detection**: Detects faces and analyzes expressions in real-time
- **Smile/Laugh Detection**: Automatically detects when subjects smile or laugh
- **Automatic Photo Capture**: Takes a photo when a smile or laugh is detected
- **No Repeat Jokes**: Tracks which jokes have been told and won't repeat any until all have been used

### Camera Features
- **Front & Back Camera Support**: Switch between cameras with one tap
- **Permission Handling**: Properly requests camera and storage permissions
- **Photo Saving**: Photos are saved locally (gallery on mobile, download on web/desktop)

### Joke Categories

| Category | Description |
|----------|-------------|
| **General** | Classic humor for all audiences |
| **Programming** | Tech and coding jokes for developers |
| **Knock-Knock** | Traditional call-and-response jokes |
| **Dad** | Family-friendly groan-worthy puns |

## Platform Details

### Android
- **Technology**: Kotlin, CameraX, Google ML Kit, Android TTS
- **Min SDK**: Android 7.0 (API 24)
- **Face Detection**: Google ML Kit with smile probability analysis
- **Distribution**: APK via GitHub Releases

### iOS
- **Technology**: Swift, SwiftUI, AVFoundation, Vision framework, AVSpeechSynthesizer
- **Min Version**: iOS 16.0
- **Face Detection**: Apple Vision framework with facial landmark analysis
- **Distribution**: Build from source with Xcode 15+

### Web
- **Technology**: HTML5, CSS3, JavaScript (no framework dependencies)
- **Camera**: WebRTC / getUserMedia API
- **Face Detection**: face-api.js (TensorFlow.js-based)
- **Speech**: Web Speech API (SpeechSynthesis)
- **Distribution**: Static files - deploy to any web server, or open `index.html` locally
- **Hosting**: Can be deployed to GitHub Pages automatically

### Desktop (Windows, macOS, Linux)
- **Technology**: Electron wrapping the Web version
- **Camera**: Same WebRTC-based camera as web version
- **Face Detection**: Same face-api.js as web version
- **Speech**: Same Web Speech API as web version
- **Distribution**:
  - **Windows**: NSIS installer (`.exe`) and portable
  - **macOS**: DMG disk image (x64 + arm64)
  - **Linux**: AppImage and `.deb` package

## Configuration Options

### Joke Category Filtering
- Enable or disable each joke category via checkboxes
- At least one category must always be selected

### Manual Joke Mode
- When enabled, shows a "Tell a Joke" button
- Allows telling one joke at a time manually

### Smile/Laugh Detection Toggle
- Enable or disable automatic face detection
- When disabled, photos must be taken manually

### Timer Mode
- Take photos after a configurable delay instead of waiting for smile/laugh detection
- Adjustable timer from 0.5 to 10.0 seconds

### Detection Mode
- **Smile Only**: Trigger on any smile
- **Laugh Only**: Trigger only on big smiles (laughs)
- **Smile AND Laugh**: Requires detecting a smile first, then a bigger laugh
- **Smile OR Laugh** (Default): Triggers on either a smile or laugh

### Timing Settings
- **Wait Time Before Next Joke**: 0.5-10 seconds (default: 2.5)
- **Setup to Punchline Delay**: 0-2 seconds (default: 0.81)

### Joke Management
- **Export**: Save current joke collection as JSON
- **Import**: Load custom jokes from JSON files
- **Reset**: Mark all jokes as untold
- **Custom Categories**: Create your own categories when importing

## Custom Jokes

### JSON Format
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
  }
]
```

## How It Works

### Automatic Mode
1. Press "Start Auto" to begin
2. The app tells a joke (setup, pause, punchline)
3. After the joke, it waits for a smile or laugh
4. When detected, it automatically takes a photo
5. If no reaction within the wait time, it tells another joke
6. Continues until you press "Stop"

### Manual Mode
1. Enable "Manual Joke Mode" in settings
2. Press "Tell a Joke" to hear one joke
3. Capture manually or wait for detection

## Installation

### Android
Download the latest APK from the [Releases](https://github.com/socrtwo/JokeCamera/releases) page.

### iOS
1. Open `ios/JokeCamera.xcodeproj` in Xcode 15+
2. Select your development team for signing
3. Build and run on your device

### Web
1. Download `JokeCamera-web.zip` from [Releases](https://github.com/socrtwo/JokeCamera/releases)
2. Extract and open `index.html` in a modern browser
3. Or deploy the `web/` folder to any static web host

### Windows
Download the `.exe` installer from [Releases](https://github.com/socrtwo/JokeCamera/releases).

### macOS
Download the `.dmg` from [Releases](https://github.com/socrtwo/JokeCamera/releases).

### Linux
Download the `.AppImage` or `.deb` from [Releases](https://github.com/socrtwo/JokeCamera/releases).

### Desktop (from source)
```bash
cd desktop
npm install
npm start          # Run in development
npm run build:win  # Build Windows installer
npm run build:mac  # Build macOS DMG
npm run build:linux # Build Linux packages
```

## Building from Source

### Android
```bash
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/`

**Requirements**: JDK 17, Android SDK 34

### iOS
```bash
cd ios
xcodebuild build -project JokeCamera.xcodeproj -scheme JokeCamera \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```

**Requirements**: Xcode 15.3+, macOS

### Web
No build step required - static HTML/CSS/JS files. Just serve the `web/` directory.

### Desktop
```bash
cd desktop
npm install
npm run build  # Builds for current platform
```

**Requirements**: Node.js 20+

## Project Structure

```
JokeCamera/
├── app/                          # Android (Kotlin)
│   ├── src/main/
│   │   ├── java/com/jokecamera/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SettingsActivity.kt
│   │   │   ├── JokeManager.kt
│   │   │   └── FaceAnalyzer.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── ios/                          # iOS (Swift/SwiftUI)
│   ├── JokeCamera.xcodeproj/
│   └── JokeCamera/
│       ├── JokeCameraApp.swift
│       ├── ContentView.swift
│       ├── CameraManager.swift
│       ├── FaceDetector.swift
│       ├── JokeManager.swift
│       ├── SpeechManager.swift
│       ├── SettingsView.swift
│       └── jokes.json
├── web/                          # Web (HTML/CSS/JS)
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── app.js
│       ├── jokes-data.js
│       ├── joke-manager.js
│       ├── face-detector.js
│       └── speech.js
├── desktop/                      # Desktop/Electron (Win/Mac/Linux)
│   ├── package.json
│   ├── main.js
│   └── preload.js
├── .github/workflows/
│   ├── build.yml                 # Android CI
│   ├── build-release.yml         # Multi-platform release
│   ├── build-web.yml             # Web CI + GitHub Pages
│   ├── build-desktop.yml         # Desktop CI
│   └── build-ios.yml             # iOS CI
├── build.gradle
├── settings.gradle
└── README.md
```

## Tips for Best Results

1. **Lighting**: Good lighting helps face detection work better
2. **Distance**: Keep subjects 2-6 feet from the camera
3. **Angle**: Face the camera directly for best detection
4. **Multiple Faces**: The app can detect multiple faces simultaneously
5. **Browser**: For web version, use Chrome or Edge for best Web Speech API support
6. **Keyboard (Web/Desktop)**:
   - `Space` — Take photo
   - `J` — Tell a joke
   - `S` — Start / stop auto mode
   - `C` — Switch camera
   - `,` — Open settings
   - `Esc` — Close settings

## License

This project is provided for personal and educational use.

## Version History

- **3.1.0**: Fun & modern web/desktop refresh
  - Modernized web UI: glassmorphism cards, animated gradient orbs, splash screen
  - Floating emoji reactions when smiles/laughs are detected
  - Confetti bursts on laugh detection (toggle-able)
  - Generated sound effects: shutter click, laugh chime, start/stop tones
  - Live happiness meter showing smile probability in real time
  - Inline photo gallery strip with tap-to-preview modal and download
  - Keyboard shortcuts: **Space** capture · **J** joke · **S** start/stop · **C** switch camera · **,** settings
  - Voice speed and pitch sliders
  - Toast notifications for feedback
  - Punchline appears with a pop animation synced to TTS
  - Stress-tested with 452 jokes, 50-cycle settings toggling, 100 confetti bursts — no leaks, <3 MB JS heap

- **3.0.0**: Multi-platform release
  - Added iOS version (Swift/SwiftUI, Vision framework)
  - Added Web version (HTML5, face-api.js, Web Speech API)
  - Added Desktop version via Electron (Windows, macOS, Linux)
  - Multi-platform CI/CD with GitHub Actions
  - GitHub Pages deployment for web version
- **2.0.0**: Major update
  - Replaced 1000 jokes with 451 curated Classic jokes
  - Added 4 joke categories (General, Programming, Knock-Knock, Dad)
  - Added joke category filtering with checkboxes
  - Added joke management system (import/export via JSON)
  - Added custom joke support with user-defined categories
- **1.0.0**: Initial Android release with 1000 jokes
