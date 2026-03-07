package com.beatsense.analyzer

import com.beatsense.audio.KeyDetector

/**
 * Key analyzer — wraps the existing KeyDetector as an Analyzer.
 *
 * Uses raw PCM (KeyDetector has its own windowing and DFT internally).
 * Returns HeroValue with key name and confidence.
 *
 * Note: KeyDetector still computes its own chromagram internally for now.
 * A future optimization could have it consume frame.chromagram instead,
 * but correctness comes first — the internal chromagram accumulation
 * and evidence gating are tightly coupled to the detector's own DFT.
 */
class KeyAnalyzer : Analyzer {

    override val id = "key"
    override val name = "Key"
    override val displayPriority = 10

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val key = KeyDetector.detect(frame.pcm)
        return when {
            key == null || key == "—" -> AnalyzerResult.Pending("KEY", "Accumulating...")
            else -> AnalyzerResult.HeroValue(
                value = key,
                label = "KEY",
                confidence = KeyDetector.getConfidence()
            )
        }
    }

    override fun reset() {
        KeyDetector.reset()
    }
}
