# NotebookLM Exploration Prompts for BeatSense

Prompts for Google NotebookLM. Upload the listed source documents, then paste the prompt to generate podcasts, study guides, or briefing docs.

---

## 1. Architecture Deep-Dive

**Sources to upload:** `docs/design-analyzer-pipeline.md`, `docs/beatsense-technical-overview.md`, `docs/domain-model.md`, `docs/adr/0010-analyzer-pipeline-architecture.md`

**Prompt:**

> Explain the BeatSense analyzer pipeline architecture to someone who knows programming but has never worked with audio. Start with the problem it solves — why hard-wiring each analyzer into the main activity doesn't scale — and then walk through the surface shader analogy from graphics programming: what a surface shader is, why it's a useful mental model, and how each concept maps to BeatSense (AudioFrame as shared pre-computation, Analyzer as the shader contract, AnalyzerResult as the output struct, AnalyzerRegistry as the compositor). Cover the concrete steps for adding a new analyzer — what files you create, what you don't touch — and explain why this architecture means the UI never needs per-analyzer code. End with the threading model and why analyzers must never block the audio thread.

---

## 2. DSP Algorithms Explained

**Sources to upload:** `docs/beatsense-technical-overview.md`, `docs/adr/0001-pure-kotlin-dsp.md`, `docs/adr/0002-onset-autocorrelation-for-bpm.md`, `docs/adr/0003-krumhansl-schmuckler-key-detection.md`, `docs/adr/0004-psychoacoustic-evidence-gating.md`, `docs/adr/0005-slow-chroma-decay.md`

**Prompt:**

> Generate a podcast-style conversation exploring how BeatSense teaches software to hear music. Start with the raw signal — what does audio look like as numbers? Then walk through BPM detection: what an onset is, why you autocorrelate the onset signal rather than the raw audio, and what adaptive smoothing does for display stability. Move to key detection: how a Hann window reduces spectral leakage, how the DFT builds a chromagram, what Krumhansl and Kessler's 1982 probe-tone experiments discovered about how humans perceive keys, and how Pearson correlation against 24 key profiles identifies the tonal center. Spend real time on the psychoacoustic evidence gating — why early versions hallucinated keys from a single sustained note and how requiring three active pitch classes and chromagram peakiness fixed it. The guiding principle is "if a trained musician couldn't tell the key, the algorithm says dash." Make it accessible to someone without a math or music background.

---

## 3. From Toy to Platform

**Sources to upload:** `docs/beatsense-technical-overview.md`, `docs/design-analyzer-pipeline.md`, `docs/domain-model.md`, `docs/adr/README.md`, `ROADMAP.md`

**Prompt:**

> Tell the story of how BeatSense evolved from a simple BPM detector built in a workshop session into an extensible audio analysis platform. Cover the initial build — onset detection, autocorrelation, the hurdles crossed (no Android Studio, JDK incompatibility, integer division bug in buffers-per-second). Then discuss the architectural inflection point: when key detection was added and the codebase started showing signs of scaling problems (five files touched per feature, growing parameter lists). Explain how the analyzer pipeline design resolved this by borrowing from graphics programming. What role did the domain model play — bounded contexts, ubiquitous language, the separation between Audio Capture, Signal Analysis, and Presentation? Look at the roadmap and discuss what the platform architecture enables: LUFS, chord detection, spectral analysis, even cross-platform via Kotlin Multiplatform. What decisions made early on turned out to be load-bearing?

---

## 4. SDD in Practice

**Sources to upload:** `docs/beatsense-technical-overview.md`, `CONTRIBUTING.md`, `docs/adr/README.md`, all ADR files from `docs/adr/`

**Prompt:**

> Explore how Skill-Driven Development was applied to build BeatSense. The app was built in a single workshop session using Claude Code with specialized skills across five domains: Audio Engineering (signal flow, digital audio, frequency spectrum, metering, music theory), Psychoacoustics (evidence gating for key detection), UX Design (mobile interaction, thumb-zone placement, progressive disclosure), Art and Aesthetics (the Luminous dark palette, color theory, visual hierarchy), and Applied Design Excellence (animation timing, level meter design). For each domain, explain what specific decisions the skills informed — not just that they were used, but how they shaped the actual code and design. What would have been different without domain-specific skills? How does this compare to traditional development where a single developer would need to research each domain independently? Discuss the ADRs as evidence of deliberate, skill-informed decisions rather than ad-hoc choices.

---

## 5. The Roadmap Conversation

**Sources to upload:** `ROADMAP.md`, `docs/design-analyzer-pipeline.md`, `docs/beatsense-technical-overview.md`, `docs/domain-model.md`

**Prompt:**

> Analyze the BeatSense roadmap and generate a discussion about what the project should prioritize next. The roadmap covers analysis features (LUFS, chord detection, spectral analysis, rhythm analysis), platform expansion (Google Play, iOS via Kotlin Multiplatform, CLI, web), UX improvements (history, export, spectrogram), and engineering foundations (value objects, instance-based detectors, benchmarks). Consider the dependencies: which features unlock others? Does the analyzer pipeline need to be implemented before new analyzers can be added efficiently? Is Play Store distribution more urgent than new features, or do you need a richer feature set to justify a store listing? Discuss the tension between depth (more analysis on Android) and breadth (same features on iOS, CLI, web). What are the riskiest items? What would a musician who uses this app daily want first versus what an engineer would prioritize?

---

## 6. Audio FAQ

**Sources to upload:** `docs/beatsense-technical-overview.md`, `docs/domain-model.md`, `docs/adr/0004-psychoacoustic-evidence-gating.md`, `docs/adr/0005-slow-chroma-decay.md`

**Prompt:**

> Generate a study guide covering the audio and signal processing concepts used in BeatSense. For each concept, explain what it is, why BeatSense uses it, and give an intuitive analogy. Cover these topics: RMS energy and why it matters more than peak amplitude for music analysis; onset detection and how it differs from simple threshold crossing; autocorrelation and how it finds periodicity in a signal; the DFT and how it decomposes audio into frequency components; chromagram and why folding frequencies into 12 pitch classes is useful; the Krumhansl-Schmuckler algorithm and what key profiles represent perceptually; psychoacoustic evidence gating and why three pitch classes is the minimum for key inference; LUFS versus RMS and why broadcast loudness standards matter; spectral centroid as a measure of brightness; and Hann windowing and spectral leakage. Format as question-and-answer pairs that could serve as flashcards.
