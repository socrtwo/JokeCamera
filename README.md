# Joke Camera

A cross-platform app that tells jokes and captures your reactions! Built with Kotlin Multiplatform and Compose Multiplatform, running on Android, iOS, macOS, Windows, Linux, and Web.

## Supported Platforms

| Platform | Camera + Face Detection | Text-to-Speech | Status |
|----------|------------------------|----------------|--------|
| **Android** | CameraX + ML Kit | Android TTS | Full support |
| **iOS** | Placeholder (extensible) | AVSpeechSynthesizer | Full support |
| **macOS** | - | `say` command | Full support |
| **Windows** | - | PowerShell SpeechSynthesizer | Full support |
| **Linux** | - | espeak / spd-say | Full support |
| **Web** | - | Web Speech API | Full support |

## Overview

Joke Camera combines text-to-speech joke telling with real-time face detection to capture genuine reactions. On Android, the app tells a joke, waits for your smile or laugh, and automatically snaps a photo. On other platforms, it works as a joke-telling app with manual controls and platform-native TTS.

## Features

### Core Features
- **451 Classic Jokes**: Curated collection of quality jokes organized by category
- **Text-to-Speech**: Jokes are spoken aloud using platform-native TTS engines
- **Cross-Platform UI**: Shared Compose Multiplatform UI across all platforms
- **No Repeat Jokes**: Tracks which jokes have been told and won't repeat until all have been used

### Android-Exclusive Features
- **Face Detection**: Uses Google ML Kit to detect faces and analyze expressions
- **Smile/Laugh Detection**: Automatically detects when subjects smile or laugh
- **Automatic Photo Capture**: Takes a photo when a smile or laugh is detected
- **Front & Back Camera**: Switch between cameras with one tap

### Joke Categories

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

### Timing Settings
- **Setup to Punchline Delay**: Pause between setup and punchline (0-2 seconds)
- **Wait Before Next Joke**: How long to wait before telling another joke (0.5-10 seconds)

### Detection Settings (Android)
- **Smile/Laugh Detection Toggle**: Enable/disable automatic face detection
- **Timer Mode**: Take photos after a configurable delay
- **Detection Mode**: Smile only, laugh only, both, or either

### Joke Statistics
- Shows how many jokes have been told out of the total
- Reset button to mark all jokes as untold

## Building

### Prerequisites
- JDK 17+
- Android SDK 34 (for Android builds)
- Xcode 15+ (for iOS builds, macOS only)

### Build Commands

```bash
# Android APK
./gradlew :composeApp:assembleDebug

# Desktop (auto-detects OS)
./gradlew :composeApp:run               # Run directly
./gradlew :composeApp:packageDmg         # macOS .dmg
./gradlew :composeApp:packageMsi         # Windows .msi
./gradlew :composeApp:packageDeb         # Linux .deb

# Web (Wasm)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun    # Dev server
./gradlew :composeApp:wasmJsBrowserDistribution      # Production build

# iOS
# Open iosApp/ in Xcode, or build the framework:
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

### Output Locations
- **Android APK**: `composeApp/build/outputs/apk/debug/`
- **Desktop packages**: `composeApp/build/compose/binaries/main/`
- **Web**: `composeApp/build/dist/wasmJs/productionExecutable/`

## Project Structure

```
JokeCamera/
├── composeApp/                              # Main multiplatform module
│   └── src/
│       ├── commonMain/kotlin/.../           # Shared code (all platforms)
│       │   ├── model/                       # Data models (Joke, JokeType, etc.)
│       │   ├── data/                        # JokeRepository + 451 built-in jokes
│       │   ├── platform/                    # expect declarations
│       │   └── ui/                          # Shared Compose UI
│       ├── androidMain/                     # Android: CameraX, ML Kit, TTS
│       ├── iosMain/                         # iOS: AVSpeechSynthesizer
│       ├── desktopMain/                     # Desktop: system TTS (JVM)
│       └── wasmJsMain/                      # Web: Web Speech API
├── iosApp/                                  # iOS SwiftUI wrapper
│   └── iosApp/
│       ├── ContentView.swift
│       ├── iOSApp.swift
│       └── Info.plist
├── .github/workflows/                       # CI/CD for all platforms
│   ├── build.yml                            # Build on push/PR
│   └── build-release.yml                    # Build + release on tag
├── build.gradle.kts                         # Root build config
├── composeApp/build.gradle.kts              # Multiplatform build config
├── settings.gradle.kts
└── gradle/libs.versions.toml                # Version catalog
```

## Architecture

The app uses **Kotlin Multiplatform (KMP)** with **Compose Multiplatform** for the UI:

- **commonMain**: Shared business logic (joke management, models) and shared Compose UI
- **Platform abstractions**: `expect`/`actual` pattern for platform-specific features:
  - `SettingsStore` - Key-value persistence (SharedPreferences, NSUserDefaults, java.util.prefs, localStorage)
  - `TtsEngine` - Text-to-speech (Android TTS, AVSpeechSynthesizer, system commands, Web Speech API)
  - `PlatformCameraPreview` - Camera composable (CameraX on Android, placeholder on other platforms)

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

## Installation

Download the latest builds from the [Releases](https://github.com/socrtwo/JokeCamera/releases) page:
- **Android**: `.apk` file
- **macOS**: `.dmg` file
- **Windows**: `.msi` installer
- **Linux**: `.deb` package
- **Web**: Deploy the web build to any static hosting
- **iOS**: Build from source with Xcode

## License

This project is provided for personal and educational use.

## Version History

- **3.0.0**: Cross-platform release
  - Migrated to Kotlin Multiplatform + Compose Multiplatform
  - Added iOS support (with AVSpeechSynthesizer TTS)
  - Added macOS, Windows, Linux desktop support
  - Added Web (Wasm) support with Web Speech API
  - Shared UI across all platforms using Compose Multiplatform
  - Multi-platform CI/CD with GitHub Actions
- **2.0.0**: Major update
  - Replaced 1000 jokes with 451 curated Classic jokes
  - Added 4 joke categories (General, Programming, Knock-Knock, Dad)
  - Added joke category filtering and management
  - Added custom joke support via JSON import/export
- **1.0.0**: Initial release with 1000 jokes and full configuration options
