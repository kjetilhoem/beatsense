# BeatSense — Technical Overview

## What It Is

BeatSense is a real-time audio analysis app for Android that detects **BPM (tempo)** and **musical key** from audio. It works in two modes: capturing audio output from other apps (e.g. Spotify, YouTube) via Android's AudioPlaybackCapture API, or listening through the device microphone for live music in the room.

Built in a single workshop session using Skill-Driven Development (SDD) methodology, Kotlin, Jetpack Compose, and Claude Code.

---

## Capture Modes

### App Audio (MediaProjection)
Uses Android 10+'s `AudioPlaybackCapture` API to tap into audio streams from other apps. Requires a `MediaProjection` grant (system screen-recording permission dialog). The audio is captured as PCM float, mono, 44100 Hz in a foreground service (`AudioCaptureService`) with `foregroundServiceType="mediaProjection"`. No root, no hacks — this is the official API.

### Microphone
Standard `AudioRecord` with `MediaRecorder.AudioSource.MIC`. Same sample rate, format, and buffer size. Allows BeatSense to analyze live music — someone drumming on a table, humming, or a band playing nearby. Service type includes `microphone` so it works under Android 14's stricter foreground service rules.

Both modes feed identical PCM buffers into the same analysis pipeline.

---

## BPM Detection Algorithm

**File:** `BpmDetector.kt`

1. **RMS Energy** — Computed per buffer (4096 samples at 44100 Hz = ~93ms windows, ~10.77 buffers/second).
2. **Onset Detection** — Energy history over 8 seconds. An onset is detected when current RMS exceeds `1.15x` the recent rolling average. This low threshold catches soft attacks while the averaging window rejects noise.
3. **Autocorrelation** — Applied to the onset signal (not raw audio). For each candidate lag corresponding to 55–185 BPM, the autocorrelation value is computed. The dominant lag maps directly to a BPM value.
4. **Adaptive Smoothing** — Small BPM changes (< 5 BPM) are smoothed aggressively (90/10 blend) for display stability. Larger jumps use lighter smoothing (60/40) so genuine tempo changes register quickly.
5. **Gating** — No BPM is reported until at least 3 seconds of audio have been analyzed, preventing false reads from transient noise.

**Design choice:** Autocorrelation on onsets rather than raw audio. This makes the detector robust to timbral variation — it responds to rhythmic attack patterns regardless of instrument or frequency content.

---

## Key Detection Algorithm

**File:** `KeyDetector.kt`

1. **Windowing** — Hann window applied to each 4096-sample buffer to reduce spectral leakage.
2. **DFT Chromagram** — For each frequency bin from 55 Hz (A1) to 2000 Hz, compute magnitude via DFT, then fold into 12 pitch classes (C through B). This builds a chromagram — a 12-element vector showing the energy distribution across the chromatic scale.
3. **Long-term Accumulation** — The chromagram decays slowly (`0.97 * old + 0.03 * new`), giving an effective window of ~10+ seconds. This prevents single transient notes from dominating.
4. **Krumhansl-Schmuckler Algorithm** — The accumulated chromagram is correlated (Pearson) against 24 key profiles (12 major + 12 minor) derived from Krumhansl & Kessler's 1982 probe-tone experiments. The key with the highest correlation wins.
5. **Confidence** — The gap between the best and second-best correlation, normalized. Small gap = ambiguous key = low confidence.

### Psychoacoustic Evidence Gating

Early versions hallucinated keys from insufficient evidence — a single sustained note would trigger "A Minor" with apparent confidence. This was fixed by implementing a psychoacoustic evidence gate inspired by Krumhansl & Kessler (1982):

- **Active pitch class count:** At least 3 distinct pitch classes must have energy >= 20% of the maximum. A single note or interval cannot define a key — you need a tonal hierarchy.
- **Chromagram peakiness:** Coefficient of variation (std/mean of the normalized chromagram) must exceed 0.35. A flat chromagram (all pitch classes roughly equal) indicates noise, not tonality.
- **Minimum accumulation:** 50 frames (~5 seconds) before any key is reported.
- **Re-evaluation throttle:** Key is only recalculated every 10 frames (~1 second) for display stability.
- **Confidence floor:** Results below 0.08 correlation confidence are suppressed.

**Guiding principle:** "If a trained musician couldn't tell the key, the algorithm says '—'."

---

## Audio Level Analysis

**File:** `AudioAnalyzer.kt`

RMS level with asymmetric smoothing: fast attack (30/70 blend) so transients register immediately, slow release (92/8 blend) so the meter doesn't flicker. Normalized to 0–1 for display.

---

## Architecture

```
MainActivity
  |-- MediaProjection launcher (App Audio mode)
  |-- Mic permission launcher (Microphone mode)
  |-- AudioCaptureService (foreground service)
  |     |-- AudioRecord (MIC or PlaybackCapture)
  |     |-- Callback -> BpmDetector.detect(buffer)
  |     |-- Callback -> KeyDetector.detect(buffer)
  |     |-- Callback -> AudioAnalyzer.analyze(buffer)
  |-- BeatSenseScreen (Jetpack Compose UI)
        |-- BPM card with confidence bar
        |-- Key card with confidence bar
        |-- Level meter (32 segments)
        |-- Mode selector
        |-- Start/Stop button
```

Single-activity architecture. The foreground service owns the audio pipeline and exposes a callback interface. The activity binds on start and unbinds on stop. All DSP runs on the service's audio thread; UI updates flow through Compose state.

---

## UI & Design System

**File:** `Theme.kt`, `BeatSenseScreen.kt`

### Visual Identity: "Luminous"
Dark studio aesthetic. The UI should feel like looking at professional audio equipment in a dimly lit room.

- **Surface layers:** `void (#08081A)` -> `surface0 (#0D0D1F)` -> `surface1 (#141428)` -> `surface2 (#1C1C3A)` -> `surface3 (#252545)`. Depth through subtle luminance steps.
- **Accent:** Coral `#E94560` — warm, high-contrast against the cool dark palette. Used for BPM value, active states, and the primary action button.
- **Typography:** Monospace for the BPM hero number (data feel), sans-serif elsewhere. Modular scale from 10sp captions to 72sp hero display.
- **Spacing:** 8dp grid system. Consistent 24dp corner radius on cards, 16dp on smaller elements.
- **Text hierarchy:** 87% white primary, 60% white secondary, 40% white tertiary — follows Material Design opacity guidelines for dark themes.

### Animations
- BPM value: `animateFloatAsState` with 400ms tween, ease-out — numbers glide rather than jump.
- Audio level: 100ms tween for responsive metering.
- Pulse dot: infinite transition oscillating 0.3–0.7 alpha when listening.
- Radial glow: responds to audio level, emanating from upper-center. Subtle (max 8% alpha) but creates a living, breathing feel.
- Button color: animated transition between coral (start) and surface (stop).

### Level Meter
32 horizontal segments. Green (accent) up to 60%, amber (warning) 60–85%, red above 85%. Inactive segments use surface2 — visible but recessive.

### Confidence Bars
Thin (3dp) horizontal bars. Green above 60% confidence, amber 30–60%, grey below 30%. Animated fill width.

---

## App Icon

Adaptive icon (Android 8+) using vector drawables:

- **Background:** Deep dark `#0D0D1F` with a subtle ring for depth.
- **Foreground:** Stylized audio pulse / heartbeat waveform in coral `#E94560`. The waveform has varying amplitudes (32→72→32 range) creating a recognizable EKG/audio signal shape. A small accent dot sits on the peak — the "sense" in BeatSense. A translucent glow circle (20% alpha coral) sits behind the pulse.

All vector, no raster. Scales cleanly to any density.

---

## Hurdles Crossed

1. **No Android Studio.** Entire project built from command line. Android SDK installed via `sdkmanager` to `~/Android/sdk`, avoiding Homebrew conflicts.

2. **JDK incompatibility.** System default was Java 25 (GraalVM). AGP 8.2 doesn't support it. Detected JDK 21 (Microsoft OpenJDK) via `/usr/libexec/java_home -V` and pinned builds to it.

3. **BPM not appearing.** Initial version used integer division for `BUFFERS_PER_SECOND` (44100 / 4096 = 10 instead of 10.77), causing autocorrelation lag calculations to be wrong. Fixed by using float division. Also lowered onset threshold from 1.5x to 1.15x.

4. **Key detection instability.** Early versions re-evaluated every frame with fast chroma decay, causing the key display to flicker between keys. Fixed by slowing decay to 0.97 and throttling re-evaluation to once per second.

5. **Key hallucination on single notes.** Playing a single sustained A note triggered "A Minor" detection. Implemented psychoacoustic evidence gating requiring >= 3 active pitch classes and sufficient chromagram peakiness. A single note now correctly shows "—".

6. **Missing mipmap resources.** Default project had no icon resources, causing AAPT build failures. Created adaptive icon with vector drawables.

7. **Phone disconnection during install.** USB connection dropped mid-session. Resolved on reconnection.

---

## Skills Applied (SDD)

The following skill domains were engaged during development:

- **Audio Engineering** (signal flow, digital audio, frequency spectrum, metering, music theory) — informed the entire DSP pipeline design
- **Psychoacoustics** — drove the evidence gating system for key detection
- **UX Design** (mobile, interaction design, visual design) — thumb-zone button placement, mode switching, progressive disclosure of confidence data
- **Art & Aesthetics** (color theory, visual composition) — the "Luminous" dark palette, accent color choice, visual hierarchy
- **Applied Design Excellence** (motion & polish, app aesthetics, visual harmony) — animation timing, level meter design, confidence visualization

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.01.00) |
| Min SDK | 29 (Android 10) — required for AudioPlaybackCapture |
| Target SDK | 34 (Android 14) |
| Build | AGP 8.2.2, Gradle |
| Audio | AudioRecord, AudioPlaybackCapture, PCM Float, 44100 Hz, 4096 buffer |
| DSP | Pure Kotlin — no native libraries. DFT, autocorrelation, Pearson correlation all implemented from scratch |
| Architecture | Single Activity + Foreground Service + Compose |

---

## What's Not In Here (Yet)

Ideas discussed but not implemented: harmonic/chord analysis, chord progression detection (ii-V-I patterns), dynamics analysis, genre hints, tuning deviation detection, extended stats panel.

---

*BeatSense v0.3.1 — Built at SDD Workshop, March 2026*
