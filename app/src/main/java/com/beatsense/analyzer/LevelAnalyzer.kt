package com.beatsense.analyzer

import com.beatsense.audio.AudioAnalyzer

/**
 * Level analyzer — wraps the existing AudioAnalyzer as an Analyzer.
 *
 * Uses pre-computed RMS from AudioFrame but delegates smoothing
 * to the existing AudioAnalyzer (fast attack, slow release).
 * Returns Meter result for the 32-segment level display.
 */
class LevelAnalyzer : Analyzer {

    override val id = "level"
    override val name = "Level"
    override val displayPriority = 20

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val level = AudioAnalyzer.computeLevel(frame.pcm)
        return AnalyzerResult.Meter(
            level = level,
            label = "LEVEL"
        )
    }

    override fun reset() {
        AudioAnalyzer.reset()
    }
}
