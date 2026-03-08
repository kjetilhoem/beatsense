# HeyGen Video Script — BeatSense

**Duration:** 90–120 seconds
**Presenter style:** Professional, enthusiastic but grounded. A developer who built something real and wants to show you the interesting parts.
**Pacing:** Conversational, not rushed. Let the ideas land.

---

## Script

What if your phone could hear music the way you do — not just as sound waves, but as rhythm, as tonality, as something with a key and a tempo?

That is what BeatSense does. It is a real-time audio analyzer for Android that detects BPM and musical key from any audio playing on your device — or from live music nearby through the microphone. And what makes it interesting is not just what it does, but how it was built.

**[Beat 1: Pure Kotlin DSP]**

Every piece of signal processing in BeatSense — the Fourier transforms, the onset detection, the autocorrelation, the chromagram analysis — is written in pure Kotlin. No native C libraries, no JNI, no external DSP frameworks. Just math, from scratch. At a buffer rate of about ten frames per second with 4096 samples each, it turns out you do not need a heavyweight FFT library. You just need the right algorithms and a language expressive enough to make them readable.

**[Beat 2: Psychoacoustic Evidence Gating]**

Early versions of the key detector had a problem: play a single sustained A note, and it would confidently report "A Minor." The algorithm always returns an answer, because correlation always has a maximum. The fix came from psychoacoustics research by Krumhansl and Kessler. We added an evidence gate: at least three distinct pitch classes must be active, the chromagram has to show real tonal structure — not just noise — and the system needs five seconds of accumulated data before it says anything at all. The guiding principle is simple: if a trained musician could not tell the key, the algorithm stays silent. That dash on the screen is not a failure. It is honesty.

**[Beat 3: The Surface Shader Architecture]**

As the app grew, we designed the analyzer pipeline around an analogy from graphics programming: surface shaders. In a game engine, a shader is a small self-contained function — it receives pre-computed lighting data, fills in a struct, and the engine handles the rest. BeatSense works the same way. Each analyzer receives a pre-computed AudioFrame — RMS, spectrum, chromagram — and returns a typed result. The UI renders it automatically. Adding a new analysis feature means writing one file and one line of registration. You never touch the pipeline, and you cannot break the other analyzers. It is the kind of extensibility that makes a project feel like it could actually grow.

**[Closing]**

BeatSense was built in a single workshop session using Skill-Driven Development and Claude Code as an AI pair programmer — no Android Studio, just the command line. The source code is public on GitHub.

If you are interested in signal processing, audio analysis, or just curious how far you can push pure Kotlin — check it out.

---

## HeyGen Configuration Notes

- **Avatar:** Professional male or female presenter, business-casual or developer aesthetic
- **Background:** Dark, minimal — solid dark blue-gray or subtle gradient to match the app's "Luminous" design palette
- **Voice:** Natural, moderate pace, slight enthusiasm without being salesy
- **On-screen text (optional):** Key terms can appear as lower-thirds: "Pure Kotlin DSP", "Psychoacoustic Evidence Gating", "Surface Shader Architecture"
- **Call-to-action overlay:** GitHub logo + "github.com/kjetilhoem/beatsense" at the end
