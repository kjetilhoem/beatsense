# BeatSense Roadmap

Living document. Items are grouped by theme, not priority. Status markers:

- **Planned** — defined, not started
- **In Progress** — actively being worked on
- **Done** — shipped

---

## Analysis Features

### Audio Metrics
| Feature | Description | Status |
|---------|-------------|--------|
| LUFS metering | Integrated loudness (LUFS-I), short-term (LUFS-S), momentary (LUFS-M) per EBU R128 | Planned |
| Dynamic range | Difference between loudest and quietest passages, measured over time | Planned |
| Transient density | Count and intensity of detected onsets per unit time — how "busy" the rhythm is | Planned |
| Crest factor | Peak-to-RMS ratio — indicates how compressed or dynamic the audio is | Planned |

### Tonal Analysis
| Feature | Description | Status |
|---------|-------------|--------|
| Separate mode from key | Report root note and mode (Major/Minor) independently, allowing mode to update without root changing | Planned |
| Chord detection | Identify chords (triads, 7ths) from the chromagram in real time | Planned |
| Chord progression tracking | Detect common progressions (ii-V-I, I-IV-V, etc.) and display as Roman numerals | Planned |
| Tuning reference detection | Detect whether A=440 Hz or offset (e.g., A=432 Hz, A=442 Hz) | Planned |
| Scale suggestion | Given detected pitch classes, suggest likely scales (pentatonic, blues, modes) | Planned |

### Spectral Analysis
| Feature | Description | Status |
|---------|-------------|--------|
| Frequency band energy | Energy in sub-bass (<60 Hz), bass (60-250), mids (250-4k), presence (4-8k), air (8k+) | Planned |
| Spectral centroid | Brightness of the audio — single number indicating where the spectral weight sits | Planned |
| Spectral rolloff | Frequency below which N% of spectral energy is contained — darkness/brightness indicator | Planned |
| Stereo imaging | Visualize stereo width, L/R balance, and mid/side energy — requires stereo capture | Planned |

### Rhythm Analysis
| Feature | Description | Status |
|---------|-------------|--------|
| Time signature estimation | Detect 3/4 vs 4/4 vs other meters from accent patterns | Planned |
| Swing/groove quantification | Measure the swing ratio (straight vs. shuffled feel) | Planned |
| BPM tap-to-confirm | Manual tap mode to cross-reference detected BPM | Planned |

---

## Platform & Distribution

### Android
| Milestone | Description | Status |
|-----------|-------------|--------|
| Google Play developer account | Register, verify identity, pay $25 fee | Planned |
| App signing & AAB build | Release keystore, signed Android App Bundle | Planned |
| Privacy policy | Hosted page (GitHub Pages) stating all processing is on-device | Planned |
| Store listing | Screenshots, feature graphic, descriptions | Planned |
| Closed testing (20 testers, 14 days) | Required for new accounts before production access | Planned |
| Play Store production release | Public listing on Google Play | Planned |
| Staged rollouts | 1% → 10% → 100% with crash rate monitoring | Planned |

### iOS
| Milestone | Description | Status |
|-----------|-------------|--------|
| Kotlin Multiplatform evaluation | Assess sharing DSP core between Android and iOS | Planned |
| iOS audio capture | AVAudioEngine / AVAudioSession for mic and system audio | Planned |
| SwiftUI presentation layer | Native iOS UI matching the Luminous design system | Planned |
| App Store submission | Developer account, review, TestFlight | Planned |

### CLI
| Milestone | Description | Status |
|-----------|-------------|--------|
| CLI analyzer | Same DSP pipeline, reads from audio files or system audio input | Planned |
| File analysis mode | Analyze WAV/FLAC/MP3 files, output BPM + key + stats | Planned |
| Pipe-friendly output | JSON or plain text output for scripting and piping | Planned |
| Real-time mode | Read from audio input device (ALSA/PulseAudio/CoreAudio) | Planned |
| Cross-platform build | Kotlin/Native or Kotlin/JVM — runs on macOS, Linux, Windows | Planned |

### Web
| Milestone | Description | Status |
|-----------|-------------|--------|
| Web Audio API prototype | Browser-based analyzer using the same algorithms via Kotlin/JS or WASM | Planned |
| Drag-and-drop file analysis | Upload or drag audio files for analysis | Planned |

---

## UX & Presentation

| Feature | Description | Status |
|---------|-------------|--------|
| Analysis history | Persist past sessions with timestamps, BPM, key, stats | Planned |
| Export/share results | Share analysis as text, image, or structured data | Planned |
| Expanded stats panel | Collapsible panel showing LUFS, dynamic range, spectral data | Planned |
| Landscape mode | Optimized layout with waveform or spectrogram visualization | Planned |
| Spectrogram view | Real-time frequency × time visualization | Planned |
| Dark/light theme toggle | Luminous dark is the default, but offer a light option | Planned |
| Accessibility audit | Verify contrast ratios, screen reader support, touch targets | Planned |

---

## Engineering

| Item | Description | Status |
|------|-------------|--------|
| GitHub Actions CI | Automated build + test on every push to main | Planned |
| Instrumented tests | Android emulator tests for service lifecycle and permissions | Planned |
| Code coverage reporting | Track test coverage for the DSP core | Planned |
| Explicit value objects | Replace primitive Float/String with typed Bpm, MusicalKey, Confidence | Planned |
| Instance-based detectors | Replace singleton objects with instances for multi-session support | Planned |
| Benchmark suite | Performance tests for DSP pipeline on target devices | Planned |

---

## How to Propose Changes

Open an issue or PR. Reference this roadmap if the work relates to an existing item. For new ideas, add them here via PR — the roadmap is a living document, not a contract.
