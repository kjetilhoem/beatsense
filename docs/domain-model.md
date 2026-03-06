# BeatSense Domain Model

Domain-Driven Design documentation following Eric Evans' principles.

## Ubiquitous Language

These terms are used consistently in code, documentation, and conversation.

| Term | Definition |
|------|-----------|
| **Onset** | A detected rhythmic attack — a moment where audio energy rises significantly above the local average |
| **Autocorrelation** | A signal processing technique that finds repeating patterns by comparing a signal with delayed copies of itself |
| **Chromagram** | A 12-element vector representing energy distribution across the chromatic pitch classes (C through B) |
| **Pitch class** | One of the 12 notes in the chromatic scale, regardless of octave. A4 (440 Hz) and A2 (110 Hz) are the same pitch class |
| **Tonal evidence** | Sufficient harmonic content in the chromagram to justify key detection — requires >= 3 active pitch classes and non-flat distribution |
| **Peakiness** | How much a chromagram deviates from uniform (noise). Measured as coefficient of variation. High peakiness = tonal content |
| **Key profile** | A 12-element template representing the expected pitch class distribution for a given key (Krumhansl-Kessler profiles) |
| **Confidence** | The gap between the best and second-best match, normalized to 0–1. Low confidence = ambiguous result |
| **Capture mode** | The audio source: App Audio (other apps via MediaProjection) or Microphone (live audio via mic) |
| **Audio level** | RMS energy of the signal, smoothed with asymmetric attack/release, normalized to 0–1 |
| **BPM** | Beats per minute — the tempo of the music, derived from onset periodicity |
| **Musical key** | The tonal center and mode (e.g., "C Major", "A Minor") derived from chromagram correlation with key profiles |
| **Evidence gate** | The psychoacoustic check that prevents key detection from reporting results without sufficient tonal evidence |

## Bounded Contexts

### 1. Audio Capture

**Responsibility:** Acquire raw audio from the platform and deliver it as PCM buffers.

**Language:** sample rate, buffer size, PCM float, capture mode, foreground service, MediaProjection, AudioRecord

**Key classes:**
- `AudioCaptureService` — the Android foreground service that owns the audio pipeline
- `AudioConfig` — shared constants (sample rate, buffer size)
- `CaptureMode` — enum: APP_AUDIO, MICROPHONE

**Boundaries:** This context speaks Android framework language (Service, Intent, Notification). It produces raw `FloatArray` buffers. The downstream contexts never see Android APIs.

### 2. Signal Analysis (Core Domain)

**Responsibility:** Transform raw audio into musical meaning — BPM, key, level.

This is the **core domain**. It contains the business logic that makes BeatSense valuable. It has no Android dependencies and is fully testable on JVM.

**Language:** onset, autocorrelation, chromagram, pitch class, tonal evidence, key profile, confidence, peakiness, evidence gate, BPM, musical key

**Key classes:**
- `BpmDetector` — onset detection + autocorrelation → BPM
- `KeyDetector` — chromagram + Krumhansl-Schmuckler + evidence gating → musical key
- `AudioAnalyzer` — RMS level computation with VU-meter smoothing → audio level

**Invariants:**
- BPM is only reported after >= 3 seconds of audio (no premature guessing)
- BPM is always in the range 55–185 or zero (not detected)
- Key is only reported when tonal evidence is sufficient (>= 3 active pitch classes, peakiness > 0.35)
- Confidence is always 0–1
- Audio level is always 0–1

**Domain events (implicit, communicated via return values):**
- BPM detected (float > 0)
- Key detected (non-"—" string)
- Tonal evidence insufficient (returns "—")
- No onset activity (returns 0 BPM)

### 3. Presentation

**Responsibility:** Render the analysis results as a responsive, animated UI.

**Language:** card, hero display, confidence bar, level meter, mode selector, pulse, glow, accent

**Key classes:**
- `BeatSenseScreen` — the main Compose screen
- `BeatSenseTheme` — the design system (colors, spacing, typography)
- `ConfidenceBar` — visual confidence indicator
- `LevelMeter` — 32-segment audio level display

**Boundaries:** Receives simple values (Float, String, Boolean) from the application layer. Never references DSP classes directly.

## Context Map

```
┌──────────────────┐         ┌──────────────────────┐         ┌─────────────────┐
│  Audio Capture   │         │   Signal Analysis    │         │  Presentation   │
│                  │         │   (Core Domain)      │         │                 │
│  AudioCapture    │ FloatArray │  BpmDetector       │ Float   │  BeatSenseScreen│
│  Service         │────────>│  KeyDetector         │────────>│  Theme          │
│  AudioConfig     │         │  AudioAnalyzer       │ String  │  ConfidenceBar  │
│  CaptureMode     │         │                      │ Float   │  LevelMeter     │
│                  │         │  AudioConfig         │         │                 │
└──────────────────┘         └──────────────────────┘         └─────────────────┘
   Infrastructure                    Domain                      Presentation
   (Android APIs)              (Pure Kotlin, no deps)          (Jetpack Compose)
```

**Relationships:**
- Audio Capture → Signal Analysis: **Customer-Supplier**. Capture supplies raw PCM; Analysis defines what it needs (sample rate, buffer size via AudioConfig).
- Signal Analysis → Presentation: **Published Language**. Analysis publishes simple primitives (Float for BPM/level/confidence, String for key). Presentation consumes without knowing how they were computed.

## Aggregate: Analysis Session

The implicit aggregate in BeatSense is the analysis session — the period between pressing Start and Stop.

```
AnalysisSession (Aggregate Root)
├── CaptureMode (Value Object)
├── BpmDetector state
│   ├── energyHistory: FloatArray
│   ├── onsetSignal: FloatArray
│   ├── smoothedBpm: Float
│   └── confidence: Float
├── KeyDetector state
│   ├── chromaLong: FloatArray
│   ├── lastKey: String
│   └── confidence: Float
└── AudioAnalyzer state
    └── smoothedLevel: Float
```

Currently this is implicit (singleton objects with mutable state, reset between sessions). A future refactoring could make it explicit — each session creates fresh detector instances.

## Value Objects (candidates for future extraction)

| Value Object | Fields | Invariants |
|---|---|---|
| `Bpm` | value: Float | 0 (not detected) or 55–185 |
| `MusicalKey` | root: String, mode: Mode | root in NOTE_NAMES, mode in {Major, Minor} |
| `Confidence` | value: Float | 0.0–1.0 |
| `AudioLevel` | value: Float | 0.0–1.0 |
| `TonalEvidence` | sufficient: Boolean, activePitchClasses: Int, peakiness: Float | Already exists in KeyDetector |

## Design Decisions

The domain model is intentionally lightweight for a v0.3 app:
- Detectors are singletons (`object`) rather than instances — simpler, but means only one session at a time
- Domain events are implicit (return values) rather than explicit event objects
- No repository layer — state lives in memory only for the duration of a session
- Value objects are primitives (Float, String) rather than typed wrappers

These are deliberate tradeoffs for a workshop-built app. If BeatSense grows to support session history, export, or multi-track analysis, the model should evolve toward explicit aggregates, typed value objects, and domain events.
