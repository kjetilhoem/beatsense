package com.beatsense.analyzer

import com.beatsense.audio.BpmDetector

/**
 * BPM analyzer — wraps the existing BpmDetector as an Analyzer.
 *
 * Uses raw PCM (onset detection works on energy, not spectrum),
 * returns HeroValue with BPM and confidence.
 */
class BpmAnalyzer : Analyzer {

    override val id = "bpm"
    override val name = "BPM"
    override val displayPriority = 0

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val bpm = BpmDetector.detect(frame.pcm)
        return if (bpm > 0f) {
            AnalyzerResult.HeroValue(
                value = "%.0f".format(bpm),
                label = "BPM",
                confidence = BpmDetector.getConfidence(),
                accentColor = true
            )
        } else {
            AnalyzerResult.Pending("BPM", "Listening...")
        }
    }

    override fun reset() {
        BpmDetector.reset()
    }
}
