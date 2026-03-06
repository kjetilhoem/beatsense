# ADR-0004: Psychoacoustic Evidence Gating for Key Detection

## Status
Accepted

## Context
Early key detection reported keys from insufficient evidence — a single sustained note would trigger "A Minor" with apparent confidence. The Krumhansl-Schmuckler algorithm will always return a best-match key, even from noise or a single pitch class, because correlation always has a maximum.

## Decision
Gate key detection behind psychoacoustic evidence thresholds before running the algorithm:
1. **Minimum pitch classes:** >= 3 active pitch classes (energy >= 20% of max). A single note or interval cannot define a key — you need a tonal hierarchy.
2. **Chromagram peakiness:** Coefficient of variation (std/mean) must exceed 0.35. Flat chromagram = noise, not tonality.
3. **Accumulation minimum:** 50 frames (~5 seconds) before any key is reported.
4. **Re-evaluation throttle:** Recalculate only every 10 frames (~1 second).
5. **Confidence floor:** Suppress results below 0.08 correlation confidence.

Guiding principle: "If a trained musician couldn't tell the key, the algorithm says '—'."

## Consequences
- **Pro:** Eliminates false positives from single notes, hum, background noise, fridge buzz
- **Pro:** Grounded in psychoacoustic research (Krumhansl & Kessler, 1982)
- **Pro:** The "—" state builds user trust — silence is more honest than a guess
- **Con:** Takes longer to report a key (~5+ seconds of tonal content)
- **Con:** Sparse music (solo melody without harmony) may never trigger a key — this is correct behavior but might frustrate users expecting an answer
