# ADR-0001: Pure Kotlin DSP — No Native Libraries

## Status
Accepted

## Context
BeatSense needs DFT, autocorrelation, and chromagram computation for real-time audio analysis. Options ranged from native C/C++ libraries (TarsosDSP, Essentia, FFTW via JNI) to writing the DSP in pure Kotlin.

## Decision
Implement all signal processing in pure Kotlin: DFT, Hann windowing, onset detection, autocorrelation, Pearson correlation, chromagram folding.

## Consequences
- **Pro:** Zero native dependencies, no JNI complexity, no ABI-specific builds
- **Pro:** Entire codebase is readable Kotlin — no context-switching to C
- **Pro:** Trivially portable and debuggable
- **Con:** Slower than FFTW or native FFT — but our buffer (4096 samples) and bin range (55–2000 Hz) are small enough that it doesn't matter at ~10 Hz processing rate
- **Con:** If we need full-spectrum high-resolution FFT later, we may revisit
