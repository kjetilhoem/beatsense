package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Crest factor analyzer — peak-to-RMS ratio.
 *
 * Indicates how compressed or dynamic the audio is:
 * - Low crest factor (~3-6 dB): heavily compressed (EDM, modern pop)
 * - Medium crest factor (~6-12 dB): moderate dynamics (rock, mixed vocals)
 * - High crest factor (>12 dB): very dynamic (classical, jazz, live recording)
 *
 * Computed as 20*log10(peak/RMS) in dB. Smoothed for stable display.
 */
class CrestFactorAnalyzer : Analyzer {

    override val id = "crest_factor"
    override val name = "Crest Factor"
    override val displayPriority = 45

    private var smoothedCrestDb = 0f
    private var frameCount = 0

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val rms = frame.rms
        if (rms < 1e-6f) {
            return if (frameCount < 5) {
                AnalyzerResult.Pending("DYNAMICS", "Listening...")
            } else {
                AnalyzerResult.HeroValue(
                    value = "—",
                    label = "DYNAMICS",
                    confidence = 0f
                )
            }
        }

        var peak = 0f
        for (sample in frame.pcm) {
            val absSample = abs(sample)
            if (absSample > peak) peak = absSample
        }

        if (peak < 1e-6f) {
            return AnalyzerResult.HeroValue(value = "—", label = "DYNAMICS", confidence = 0f)
        }

        val crestDb = 20f * kotlin.math.log10(peak / rms)

        // Smooth: 0.8 old + 0.2 new (slow, dynamics don't change fast)
        smoothedCrestDb = if (smoothedCrestDb == 0f) {
            crestDb
        } else {
            smoothedCrestDb * 0.8f + crestDb * 0.2f
        }

        val dynamicsLabel = when {
            smoothedCrestDb < 6f -> "Compressed"
            smoothedCrestDb < 12f -> "Moderate"
            else -> "Dynamic"
        }

        return AnalyzerResult.HeroValue(
            value = "$dynamicsLabel\n${String.format(Locale.US, "%.1f", smoothedCrestDb)} dB",
            label = "DYNAMICS",
            confidence = (smoothedCrestDb / 20f).coerceIn(0f, 1f)
        )
    }

    override fun reset() {
        smoothedCrestDb = 0f
        frameCount = 0
    }
}
