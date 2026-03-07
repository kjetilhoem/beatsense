# ADR-0010: Analyzer Pipeline Architecture

## Status
Accepted

## Context
Adding new analysis features (LUFS, frequency bands, chord detection) required changes in five places: analyzer class, audio callback wiring, state variables, screen parameters, and UI code. This coupling doesn't scale and risks breaking existing analyzers.

## Decision
Adopt a plugin-style architecture inspired by surface shaders in graphics programming:

1. **AudioFrame** — pre-computes shared data (RMS, FFT spectrum, chromagram) once per buffer, passed to all analyzers
2. **Analyzer interface** — each analyzer receives an AudioFrame, returns a typed AnalyzerResult. Isolated, stateful, independently testable.
3. **AnalyzerResult sealed class** — typed output variants (HeroValue, Meter, Bands, ValueGroup, Pending) that the UI renders automatically
4. **AnalyzerRegistry** — orchestrates the pipeline: builds AudioFrame, fans out to analyzers, collects results

## Consequences
- **Pro:** Adding a new analyzer requires one file and one line of registration
- **Pro:** Shared FFT computation — no redundant DFT across analyzers
- **Pro:** Analyzers are isolated and independently testable
- **Pro:** UI card rendering is generic — new result types get cards for free
- **Con:** Slight indirection vs. direct wiring
- **Con:** Existing analyzers (BpmDetector, KeyDetector) are still singletons internally — wrapped but not yet refactored to instances
- **Migration:** Incremental — existing UI bridge extracts values from results map, screen unchanged
