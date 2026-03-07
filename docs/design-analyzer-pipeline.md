# Design: Analyzer Pipeline Architecture

## The Problem

BeatSense currently has three hard-wired analyzers (BpmDetector, KeyDetector, AudioAnalyzer). Adding a new analysis feature requires changes in five places:

1. Create the analyzer class
2. Wire it into the audio callback in `MainActivity`
3. Add state variables in `MainActivity`
4. Pass them as parameters to `BeatSenseScreen`
5. Build UI for it in `BeatSenseScreen`

Every new feature touches the same files, grows the same parameter lists, and risks breaking existing analyzers. This doesn't scale.

## The Analogy: Surface Shaders

In graphics programming, a **surface shader** is a small, self-contained function that describes a surface's properties (color, roughness, emission). The developer never touches the rendering pipeline — they fill in a struct, and the engine handles lighting, shadows, and compositing.

Key properties of this model:
- **Isolation** — each shader is independent, can't break other shaders
- **Contract** — the engine defines what it needs (a struct); the shader fills it in
- **Composition** — multiple shaders contribute to the final image without knowing about each other
- **Shared computation** — the engine pre-computes expensive things (lighting, transforms) once, and every shader receives them

BeatSense should work the same way.

## The Design

### 1. AudioFrame — The Shared Pre-computation

Just as a graphics engine pre-computes lighting data and passes it to every shader, the audio pipeline should pre-compute expensive shared data once per buffer and pass it to every analyzer.

```kotlin
data class AudioFrame(
    val pcm: FloatArray,
    val sampleRate: Int,
    val bufferSize: Int,
    val rms: Float,                     // pre-computed RMS energy
    val spectrum: FloatArray,           // pre-computed FFT magnitudes (per bin)
    val chromagram: FloatArray          // pre-computed 12-bin chromagram
)
```

**Why this matters:** Today, KeyDetector computes its own DFT from scratch. If we add frequency band energy, spectral centroid, and chord detection, each would naively recompute the same FFT. The AudioFrame computes it once.

The AudioFrame is the equivalent of the graphics engine's "per-fragment data" — the pre-digested audio that every analyzer receives.

### 2. Analyzer — The Surface Shader Contract

Each analyzer is a small, self-contained unit that receives an AudioFrame and produces a typed result.

```kotlin
interface Analyzer<T : AnalyzerResult> {
    /** Unique identifier, used for registration and state lookup */
    val id: String

    /** Human-readable name shown in UI */
    val name: String

    /** Process one audio frame. Called on the audio thread — must be fast. */
    fun analyze(frame: AudioFrame): T

    /** Reset all accumulated state. Called at session start. */
    fun reset()
}
```

An analyzer:
- Receives pre-computed data (AudioFrame) — never touches raw platform APIs
- Returns a typed result — never touches UI
- Manages its own internal state (accumulators, history)
- Has no reference to other analyzers
- Is independently testable with synthetic AudioFrames

### 3. AnalyzerResult — The Output Struct

Like a surface shader filling in albedo/normal/emission, each analyzer fills in a result from a small set of display types. The UI knows how to render each type without per-analyzer UI code.

```kotlin
sealed class AnalyzerResult {
    /** A single prominent value with optional confidence (BPM, key) */
    data class HeroValue(
        val value: String,
        val label: String,
        val confidence: Float = 0f,
        val accentColor: Boolean = false
    ) : AnalyzerResult()

    /** A 0-1 level (audio level, crest factor) */
    data class Meter(
        val level: Float,
        val label: String,
        val segments: Int = 32,
        val colorZones: Boolean = true
    ) : AnalyzerResult()

    /** Multiple labeled bands (frequency bands, stereo L/R) */
    data class Bands(
        val label: String,
        val bands: List<Band>
    ) : AnalyzerResult() {
        data class Band(val name: String, val level: Float)
    }

    /** Key-value pairs (stats, metadata) */
    data class Stats(
        val label: String,
        val entries: List<Pair<String, String>>
    ) : AnalyzerResult()

    /** Multiple related values in a group (LUFS-I, LUFS-S, LUFS-M) */
    data class ValueGroup(
        val label: String,
        val values: List<LabeledValue>
    ) : AnalyzerResult() {
        data class LabeledValue(val label: String, val value: String, val unit: String = "")
    }

    /** No result yet (insufficient data, warming up) */
    data class Pending(val label: String, val reason: String = "") : AnalyzerResult()
}
```

This set covers every current and planned feature:
- BPM → `HeroValue("120", "BPM", confidence=0.85, accentColor=true)`
- Key → `HeroValue("C Major", "KEY", confidence=0.72)`
- Audio level → `Meter(0.6, "LEVEL")`
- LUFS → `ValueGroup("LOUDNESS", [("I", "-14.2", "LUFS"), ("S", "-12.8", "LUFS"), ("M", "-10.1", "LUFS")])`
- Frequency bands → `Bands("SPECTRUM", [Band("Sub", 0.3), Band("Bass", 0.7), ...])`
- Transient density → `HeroValue("12.4", "TRANSIENTS/SEC")`
- Not ready yet → `Pending("KEY", "Accumulating...")`

### 4. AnalyzerRegistry — The Compositor

The registry holds all active analyzers and orchestrates the pipeline. Like a rendering compositor that combines shader outputs into the final image.

```kotlin
class AnalyzerRegistry {
    private val analyzers = mutableListOf<Analyzer<*>>()

    fun register(analyzer: Analyzer<*>)
    fun unregister(id: String)

    /** Build an AudioFrame from raw PCM, then run all analyzers */
    fun process(buffer: FloatArray): Map<String, AnalyzerResult>

    /** Reset all analyzers (new session) */
    fun resetAll()
}
```

The `process` method:
1. Computes RMS, FFT, and chromagram once → AudioFrame
2. Calls `analyze(frame)` on each registered analyzer
3. Collects results into a map keyed by analyzer ID
4. Returns the map to the UI layer

### 5. UI Rendering — Automatic Card Generation

The UI iterates over the results map and renders the appropriate card for each result type. No per-analyzer UI code needed.

```kotlin
@Composable
fun AnalyzerCard(result: AnalyzerResult) {
    when (result) {
        is AnalyzerResult.HeroValue -> HeroValueCard(result)
        is AnalyzerResult.Meter -> MeterCard(result)
        is AnalyzerResult.Bands -> BandsCard(result)
        is AnalyzerResult.Stats -> StatsCard(result)
        is AnalyzerResult.ValueGroup -> ValueGroupCard(result)
        is AnalyzerResult.Pending -> PendingCard(result)
    }
}
```

Adding a new analyzer that returns `HeroValue` gets a card for free. No UI changes.

## Data Flow

```
                    ┌─────────────────────────────────────────┐
                    │           AnalyzerRegistry               │
                    │                                           │
  FloatArray ──────>│  1. Compute AudioFrame (RMS, FFT, chroma)│
  (from capture)    │                                           │
                    │  2. Fan out to all analyzers:             │
                    │     ┌──────────────┐                      │
                    │     │ BpmAnalyzer  │──> HeroValue         │
                    │     ├──────────────┤                      │
                    │     │ KeyAnalyzer  │──> HeroValue         │
                    │     ├──────────────┤                      │
                    │     │ LevelAnalyzer│──> Meter             │
                    │     ├──────────────┤                      │
                    │     │ LufsAnalyzer │──> ValueGroup        │
                    │     ├──────────────┤                      │
                    │     │ BandAnalyzer │──> Bands             │
                    │     └──────────────┘                      │
                    │                                           │
                    │  3. Return Map<String, AnalyzerResult>    │
                    └─────────────┬─────────────────────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────────────────────┐
                    │           BeatSenseScreen                 │
                    │                                           │
                    │  for each (id, result) in results:        │
                    │      AnalyzerCard(result)                 │
                    │                                           │
                    └───────────────────────────────────────────┘
```

## What Changes, What Stays

### Adding a new analyzer (e.g., LUFS):
1. Create `LufsAnalyzer : Analyzer<AnalyzerResult.ValueGroup>`
2. Write tests for it
3. Register it: `registry.register(LufsAnalyzer())`
4. **Done.** No UI code. No wiring. No parameter threading.

### What stays the same:
- Audio capture (AudioCaptureService) — unchanged
- UI rendering engine — unchanged
- Existing analyzers — unchanged, just refactored to implement the interface
- Design system — unchanged

## Ordering and Layout Priority

Analyzers can declare a **display priority** (integer) to control card ordering. The current BPM and Key cards are highest priority (hero position). New analyzers slot in below.

```kotlin
interface Analyzer<T : AnalyzerResult> {
    val id: String
    val name: String
    val displayPriority: Int  // lower = higher on screen. BPM=0, Key=1, Level=2, etc.
    fun analyze(frame: AudioFrame): T
    fun reset()
}
```

## Threading Model

- `AudioFrame` computation and `analyze()` calls run on the audio thread (fast, no allocations in hot path)
- Results are posted to the UI thread via Compose state
- Analyzers must not block — if an analyzer needs heavy computation, it should accumulate and compute periodically (like KeyDetector already does with its 10-frame throttle)

## Shared Computation Budget

The FFT is the most expensive shared computation. At 4096 samples, a full DFT is O(n²) ≈ 16M operations. Options:

1. **Current approach**: partial DFT (only bins 55–2000 Hz) — fast enough for key detection
2. **Full FFT**: if multiple analyzers need full-spectrum data (LUFS, spectral centroid, bands), a proper FFT (Cooley-Tukey) pays for itself quickly
3. **Lazy computation**: AudioFrame could compute spectrum lazily on first access — analyzers that don't need it (like a tap-tempo) pay nothing

Recommendation: start with eager computation of the full spectrum in AudioFrame. Profile if it becomes a bottleneck. The buffer rate is ~10 Hz — there's plenty of CPU budget.

## Migration Path

This is a refactoring, not a rewrite. Steps:

1. **Define the interfaces** — `Analyzer`, `AnalyzerResult`, `AudioFrame`
2. **Create AudioFrame builder** — extract RMS from AudioAnalyzer, FFT from KeyDetector, make shared
3. **Wrap existing detectors** — `BpmAnalyzer` wraps `BpmDetector`, returns `HeroValue`. Same logic, new interface.
4. **Create AnalyzerRegistry** — replaces the manual wiring in `MainActivity`
5. **Refactor BeatSenseScreen** — replace individual parameters with `Map<String, AnalyzerResult>`
6. **Build generic card renderer** — `when (result)` dispatch
7. **Existing tests stay green** — the analyzers' internal logic doesn't change

Each step is a separate PR. Existing behavior preserved throughout.

## Comparison to Surface Shaders

| Graphics | BeatSense |
|----------|-----------|
| Vertex transform (shared) | AudioFrame computation (RMS, FFT, chromagram) |
| Surface shader (per-material) | Analyzer (per-feature) |
| Output struct (albedo, normal, emission) | AnalyzerResult (HeroValue, Meter, Bands, ...) |
| Rendering engine (lighting, compositing) | UI card renderer (when/dispatch on result type) |
| Shader registration (material system) | AnalyzerRegistry |
| Can't break other shaders | Can't break other analyzers |
| Engine handles complexity | Pipeline handles threading, lifecycle, pre-computation |

## Open Questions

1. **Analyzer configuration** — should analyzers accept parameters (e.g., LUFS gate threshold, BPM range)? If so, a `config: Map<String, Any>` or typed config per analyzer?
2. **Analyzer dependencies** — what if chord detection wants the key detector's chromagram? Pass it through AudioFrame (preferred) or allow inter-analyzer communication (risky)?
3. **Hot-reload** — should users be able to enable/disable analyzers at runtime? Adds UI complexity but increases flexibility.
4. **Analyzer-provided UI** — some future analyzers (spectrogram) may need custom rendering that doesn't fit the standard card types. Allow an optional `@Composable` override?

## Recommendation

Implement this in the next major version bump. The migration path is incremental — each step is a PR, each PR preserves existing behavior. Start with the interfaces and AudioFrame, then wrap the existing three analyzers. Once that's stable, new analyzers (LUFS, frequency bands) become trivial to add.

The goal: **adding a new analysis feature should require exactly one new file and one line of registration.**
