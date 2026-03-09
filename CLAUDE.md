# BeatSense — Real-time Audio Analyzer

## Tech Stack
- Kotlin 1.9.22, Jetpack Compose, AGP 8.2.2
- minSdk 29, targetSdk 34
- Pure Kotlin DSP — no external signal processing libraries

## Build & Run
```bash
export ANDROID_HOME=~/Android/sdk
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

./gradlew test              # Run tests
./gradlew assembleDebug     # Build debug APK
```

## Architecture
- **Surface shader pattern**: `Analyzer` interface + `AnalyzerRegistry` compositor + `AudioFrame` shared data
- One file + one registration line to add an analyzer
- Dynamic UI rendering by `AnalyzerResult` type
- Bounded contexts: Audio Capture, Signal Analysis, Presentation

## Key Paths
- `app/src/main/java/com/beatsense/audio/` — DSP core (BPM, key, analyzers)
- `app/src/main/java/com/beatsense/ui/` — Compose UI
- `app/src/test/` — unit tests
- `docs/adr/` — architecture decision records
- `docs/domain-model.md` — DDD ubiquitous language

## Conventions
- All numeric formatting uses `Locale.US` (not system locale)
- TDD — tests first, then implementation
- ADRs for significant design decisions
- **Never push directly to main.** Always create a feature branch and open a PR.
- Branch naming: `feature/<short-description>` or `fix/<short-description>`
- PRs require review before merging
- Checkpoint commits for long autonomous tasks
- Check inbox at `~/.claude/projects/-Users-kjetil-item-workspace-beatsense/inbox.md` at session start — process before other work

## Domain Skills
For audio guidance, consult skills in `~/.claude/skills/domains/audio/`
For mobile guidance, consult skills in `~/.claude/skills/domains/mobile/`
For UX guidance, consult skills in `~/.claude/skills/domains/ux/`
