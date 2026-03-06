# Contributing to BeatSense

## Getting Started

1. Fork and clone the repo
2. Ensure you have JDK 17–21 and Android SDK (platform 34) installed
3. Run `./gradlew test` to verify your setup
4. Run `./gradlew assembleDebug` to build

See the [README](README.md) for detailed build instructions.

## Development Workflow

We follow **Continuous Delivery** (Dave Farley) and **TDD** (Kent Beck):

### Trunk-Based Development
- All work happens on `main`. No long-lived feature branches.
- Short-lived branches (< 1 day) are fine for PRs.
- Every commit should be buildable and pass all tests.

### Test-Driven Development
- **Red**: Write a failing test that specifies the behavior
- **Green**: Write the minimum code to make it pass
- **Refactor**: Clean up while keeping tests green

All DSP logic (BpmDetector, KeyDetector, AudioAnalyzer) is tested with pure JVM unit tests. No Android emulator needed.

```bash
./gradlew test
```

### Commit Messages
- Lead with what changed and why
- Reference ADRs for design decisions
- Use terms from the [ubiquitous language](docs/domain-model.md#ubiquitous-language)

### Architecture Decision Records
For significant design choices, add an ADR in `docs/adr/` following the existing format (Context, Decision, Consequences). Update `docs/adr/README.md` with the new entry.

## Code Organization

The codebase follows DDD bounded contexts:

| Context | Package | Responsibility |
|---------|---------|---------------|
| Audio Capture | `com.beatsense.audio` | Acquire raw PCM from platform APIs |
| Signal Analysis | `com.beatsense.audio` | Transform audio into BPM, key, level (core domain) |
| Presentation | `com.beatsense.ui` | Render results as animated Compose UI |

The Signal Analysis context is the core domain. It has **no Android dependencies** and is fully testable on JVM. Keep it that way.

## Domain Language

Use the [ubiquitous language](docs/domain-model.md#ubiquitous-language) in code, comments, commit messages, and PRs. If the domain expert wouldn't recognize a term, rename it.

Key terms: onset, chromagram, pitch class, tonal evidence, peakiness, evidence gate, confidence, capture mode.

## What We Value

- **Honesty over guessing** — if the algorithm can't determine a result, return "—"
- **Tests before code** — prove the behavior, then implement it
- **Simple over clever** — pure Kotlin DSP, no native libraries, no over-engineering
- **Every commit releasable** — don't break main
