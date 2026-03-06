# ADR-0005: Slow Chromagram Decay (0.97/0.03)

## Status
Accepted

## Context
Early versions used fast chroma decay, causing the key display to flicker between keys frame-by-frame. Each new buffer would dramatically shift the chromagram, and the Krumhansl-Schmuckler correlation would oscillate.

## Decision
Accumulate the chromagram with very slow decay: `chroma[i] = chroma[i] * 0.97 + current[i] * 0.03`. This gives an effective averaging window of ~10+ seconds, heavily weighting history.

## Consequences
- **Pro:** Key display is stable — doesn't flicker on every beat or transient
- **Pro:** Captures the overall tonal center of a passage, not momentary pitch content
- **Pro:** Combined with the 1-second re-evaluation throttle, the UI feels calm and confident
- **Con:** Slow to react to actual key changes (modulations) — takes several seconds to shift
- **Con:** If a piece modulates frequently, the displayed key will lag or average between keys
- **Tradeoff:** For the intended use case (identifying the key of a song), stability beats responsiveness
