# ADR-0007: Command-Line Build — No Android Studio

## Status
Accepted

## Context
The project was built during a workshop using Claude Code as the sole development tool. Android Studio was not installed and installing it would have consumed significant time and disk space.

## Decision
Build entirely from command line: `sdkmanager` for SDK components, `gradlew` for builds, `adb` for deployment. No IDE.

## Consequences
- **Pro:** Lean setup — only the SDK components actually needed (~2 GB vs ~15 GB for full Studio)
- **Pro:** Demonstrates that Android development doesn't require Android Studio
- **Pro:** All build steps are reproducible and scriptable
- **Con:** No Compose previews — must deploy to device to see UI changes
- **Con:** No layout inspector, profiler, or logcat UI — raw `adb logcat` only
- **Con:** JDK version compatibility had to be discovered manually (Java 25 incompatible with AGP 8.2)
- **Lesson:** Pin JDK explicitly: `JAVA_HOME=$(/usr/libexec/java_home -v 21)`
