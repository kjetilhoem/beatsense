# ADR-0006: Dual Capture Modes — App Audio and Microphone

## Status
Accepted

## Context
The initial design only captured audio from other apps via MediaProjection (AudioPlaybackCapture API, Android 10+). This requires the user to grant screen recording permission and only works for audio playing on the device. It doesn't cover live music scenarios.

## Decision
Support two capture modes in the same service:
- **App Audio:** AudioPlaybackCapture via MediaProjection — captures Spotify, YouTube, etc.
- **Microphone:** Standard AudioRecord with MIC source — captures live music nearby

Both modes produce identical PCM float buffers (44100 Hz, mono, 4096 samples) and feed into the same analysis pipeline. The user selects the mode before starting capture.

## Consequences
- **Pro:** Covers both use cases — analyzing recorded music and live performance
- **Pro:** Same DSP pipeline, no code duplication
- **Pro:** Foreground service supports both service types (`mediaProjection|microphone`)
- **Con:** Two different permission flows (MediaProjection intent vs RECORD_AUDIO runtime permission)
- **Con:** Microphone mode picks up ambient noise, which the psychoacoustic gating handles gracefully
- **Con:** Cannot switch modes while capturing — must stop and restart
