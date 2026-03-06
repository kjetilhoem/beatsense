# ADR-0002: Onset Detection + Autocorrelation for BPM

## Status
Accepted

## Context
BPM detection can be done several ways: FFT of the amplitude envelope, beat tracking via dynamic programming (Ellis 2007), tap-tempo, or onset detection followed by autocorrelation of the onset signal.

## Decision
Use RMS energy to detect onsets (threshold: 1.15x rolling average), then autocorrelate the onset signal over an 8-second window. The dominant autocorrelation lag maps to BPM.

## Consequences
- **Pro:** Responds to rhythmic attack patterns regardless of timbre — works for drums, bass, vocals, full mixes
- **Pro:** Simple to implement and reason about
- **Pro:** Autocorrelation naturally finds the dominant periodicity even with missed onsets
- **Con:** Low onset threshold (1.15x) can trigger on non-rhythmic transients — mitigated by the autocorrelation averaging
- **Con:** Needs ~3 seconds of audio before reporting — acceptable tradeoff for accuracy
- **Note:** Adaptive smoothing (90/10 for small changes, 60/40 for large) keeps the display stable without hiding genuine tempo changes
