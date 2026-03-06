# BeatSense

Real-time BPM and musical key detection for Android.

BeatSense analyzes audio in real time to detect **tempo (BPM)** and **musical key**, using pure Kotlin signal processing with no external DSP libraries. It can capture audio from other apps (Spotify, YouTube, etc.) or listen through the microphone for live music nearby.

## Features

- **BPM detection** via onset detection + autocorrelation
- **Musical key detection** via chromagram analysis + Krumhansl-Schmuckler algorithm
- **Psychoacoustic evidence gating** — won't guess a key from insufficient harmonic evidence
- **Dual capture modes** — app audio (MediaProjection) or microphone
- **Live audio level metering** with VU-style smoothing
- **Confidence indicators** for both BPM and key

## Requirements

- Android 10+ (API 29) — required for AudioPlaybackCapture
- JDK 17–21 (JDK 25 is not compatible with AGP 8.2)
- Android SDK with platform 34 and build-tools 34.0.0

## Building

No Android Studio required. Build entirely from command line:

```bash
# Set environment (adjust paths for your system)
export ANDROID_HOME=~/Android/sdk
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS

# Run tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Install to connected device
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### SDK setup (if needed)

```bash
# Download Android command-line tools from https://developer.android.com/studio#command-tools
# Then install required packages:
sdkmanager --sdk_root=~/Android/sdk "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## Project Structure

```
app/src/main/java/com/beatsense/
├── MainActivity.kt                 # Single activity, Compose UI host
├── audio/
│   ├── AudioConfig.kt              # Shared constants (sample rate, buffer size)
│   ├── AudioCaptureService.kt      # Foreground service, audio pipeline
│   ├── BpmDetector.kt              # Onset detection + autocorrelation
│   ├── KeyDetector.kt              # Chromagram + Krumhansl-Schmuckler
│   └── AudioAnalyzer.kt            # RMS level with VU-meter smoothing
└── ui/
    ├── BeatSenseScreen.kt          # Main Compose screen
    ├── Theme.kt                    # "Luminous" design system
    └── CaptureMode.kt              # APP_AUDIO / MICROPHONE enum

app/src/test/java/com/beatsense/audio/
├── BpmDetectorTest.kt              # BPM detection tests
├── KeyDetectorTest.kt              # Key detection + evidence gating tests
└── AudioAnalyzerTest.kt            # Level metering tests
```

## How It Works

### BPM Detection
RMS energy per buffer (4096 samples at 44100 Hz) feeds an onset detector (1.15x threshold over rolling average). The onset signal is autocorrelated over an 8-second window to find the dominant periodicity, which maps to BPM. Adaptive smoothing keeps the display stable.

### Key Detection
Each buffer is Hann-windowed, transformed via DFT, and folded into a 12-bin chromagram. The chromagram accumulates slowly (0.97 decay) for a ~10-second effective window. The Krumhansl-Schmuckler algorithm correlates the chromagram against 24 key profiles (Krumhansl & Kessler, 1982). A psychoacoustic evidence gate requires >= 3 active pitch classes and sufficient chromagram peakiness before reporting a key — a single note correctly returns "—".

## Documentation

- [Architecture Decision Records](docs/adr/) — 9 ADRs documenting key design choices
- [Domain Model](docs/domain-model.md) — DDD-style bounded contexts and ubiquitous language
- [Technical Overview](docs/beatsense-technical-overview.md) — detailed description of algorithms, UI, and design

## Development Practices

- **TDD** — tests first, then implementation. 21 unit tests covering the DSP core.
- **DDD** — domain model with ubiquitous language, bounded contexts (Audio Capture, Signal Analysis, Presentation)
- **Continuous Delivery** — every commit is buildable and testable. Trunk-based development on `main`.
- **ADRs** — architectural decisions are documented, not tribal knowledge.

## Contributing

1. Fork the repo
2. Work on `main` (trunk-based development — no long-lived feature branches)
3. Write tests first (TDD — red/green/refactor)
4. Ensure `./gradlew test` passes before committing
5. Use the [ubiquitous language](docs/domain-model.md#ubiquitous-language) in code and commit messages
6. Add an [ADR](docs/adr/) for significant design decisions
7. Open a PR with a clear description

## License

MIT
