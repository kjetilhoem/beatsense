# ADR-0003: Krumhansl-Schmuckler Algorithm for Key Detection

## Status
Accepted

## Context
Musical key detection requires mapping audio frequency content to one of 24 keys (12 major + 12 minor). Approaches include template matching against key profiles, hidden Markov models, and neural networks.

## Decision
Use the Krumhansl-Schmuckler algorithm: fold DFT magnitudes into a 12-bin chromagram, then correlate (Pearson) against the 24 Krumhansl-Kessler probe-tone profiles. Highest correlation wins.

## Consequences
- **Pro:** Well-studied algorithm with decades of musicological validation (Krumhansl & Kessler, 1982)
- **Pro:** Deterministic, interpretable, no training data needed
- **Pro:** Confidence metric falls out naturally (gap between best and second-best correlation)
- **Con:** Assumes Western tonal music — won't work for atonal, microtonal, or non-Western scales
- **Con:** Relative major/minor ambiguity (C major vs A minor share the same pitch classes) — mitigated by the profile differences but still the weakest link
