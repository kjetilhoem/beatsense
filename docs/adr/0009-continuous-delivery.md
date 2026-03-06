# ADR-0009: Continuous Delivery

## Status
Accepted

## Context
BeatSense needs a delivery approach that supports rapid iteration — the app was built in a workshop setting where feedback loops must be short. Dave Farley's Continuous Delivery principles apply: every commit should be a release candidate, automation over manual process, trunk-based development.

## Decision
Adopt Continuous Delivery as the delivery philosophy:

1. **Trunk-based development** — all work on `main`, no long-lived branches.
2. **Every commit is buildable and testable** — `./gradlew test` and `./gradlew assembleDebug` must pass.
3. **Automated test suite** — unit tests for all DSP logic (BpmDetector, KeyDetector, AudioAnalyzer), run on every build.
4. **Single artifact** — the APK/AAB is built once and deployed to device or Play Store. Never rebuilt between stages.
5. **Scripted deployment** — `adb install` for local testing, Play Console upload for distribution. No manual build steps.

Current pipeline:
```
git commit → ./gradlew test → ./gradlew assembleDebug → adb install
```

Target pipeline (with Play Store):
```
git commit → ./gradlew test → ./gradlew bundleRelease → Play Console upload → Internal track → Staged rollout
```

## Consequences
- **Pro:** Feedback loops measured in minutes, not days
- **Pro:** Confidence from automated tests — refactoring is safe
- **Pro:** No merge conflicts (trunk-based, single developer currently)
- **Pro:** Any commit can be demonstrated or released
- **Con:** No CI server yet — pipeline runs locally. GitHub Actions is the natural next step.
- **Con:** Android's Play Store review adds latency to the production release stage (1-3 days)
- **Con:** No instrumented/UI tests yet — only JVM unit tests. Device testing is manual.
- **Tradeoff:** CD for mobile requires staged rollouts and monitoring since instant rollback isn't possible
