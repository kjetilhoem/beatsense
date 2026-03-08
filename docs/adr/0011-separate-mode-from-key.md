# ADR-0011: Separate Mode from Key

## Status
Accepted

## Context
The KeyDetector returned a combined string like "C Major" — root note and mode fused into a single value. This caused two problems:

1. **UI coupling:** root and mode could not be displayed with different visual weight. Both are important, but the root note is the primary identifier. A musician scanning the screen wants to see "C" first, then "Major" as qualifying context.
2. **Update granularity:** when new evidence shifts the mode (Major → Minor) but confirms the same root, the entire string changes. The visual jump ("C Major" → "C Minor") is disproportionate to the information change. Separating them lets the root stay visually stable while mode updates independently.

Psychoacoustically, root identification and mode perception are distinct cognitive processes. Root is tied to pitch memory (fast, categorical). Mode is tied to interval quality perception (slower, requires harmonic context). The UI should reflect this asymmetry.

## Decision
1. Add `KeyResult` data class to KeyDetector with separate `root: String?` and `mode: String?` fields
2. Add `getKeyResult()` method alongside existing `detect()` for backward compatibility
3. KeyAnalyzer returns `AnalyzerResult.ValueGroup` with two `LabeledValue` entries: Root and Mode
4. UI displays root note prominently (large text) with mode as secondary text below

## Consequences
- **Pro:** Root and mode can update independently — less visual noise
- **Pro:** Better information hierarchy in the UI (root = identity, mode = quality)
- **Pro:** Backward compatible — `detect()` still returns the combined string
- **Pro:** Enables future features like mode-only tracking (e.g., "this section went minor")
- **Con:** KeyAnalyzer now returns ValueGroup instead of HeroValue, requiring UI handling for the new type
- **Con:** KeyDetector tracks additional state (lastRoot, lastMode)
