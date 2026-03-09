package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

/**
 * Crest factor analyzer — peak-to-RMS ratio.
 *
 * Indicates how compressed or dynamic the audio is.
 * Low crest factor = heavily compressed (loud, no peaks).
 * High crest factor = dynamic (peaks stand out from average).
 */
class CrestFactorAnalyzer : Analyzer {

    override val id = "crest_factor"
    override val name = "Dynamics"
    override val displayPriority = 50

    private var smoothedCrest = 0f
    private var frameCount = 0

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val rms = frame.rms
        if (rms < 1e-6f) {
            return if (frameCount < 5) {
                AnalyzerResult.Pending("DYNAMICS", "Listening...")
            } else {
                AnalyzerResult.HeroValue(value = "—", label = "DYNAMICS", confidence = 0f)
            }
        }

        // Find peak amplitude
        var peak = 0f
        for (sample in frame.pcm) {
            val v = abs(sample)
            if (v > peak) peak = v
        }

        if (peak < 1e-6f) {
            return AnalyzerResult.HeroValue(value = "—", label = "DYNAMICS", confidence = 0f)
        }

        val crestDb = 20f * log10(peak / rms)

        // Exponential smoothing: 0.8 old + 0.2 new (dynamics change slowly)
        smoothedCrest = if (smoothedCrest == 0f) {
            crestDb
        } else {
            smoothedCrest * 0.8f + crestDb * 0.2f
        }

        val label = when {
            smoothedCrest < 6f -> "Compressed"
            smoothedCrest < 12f -> "Moderate"
            else -> "Dynamic"
        }

        val confidence = (rms / 0.1f).coerceIn(0f, 1f)

        return AnalyzerResult.HeroValue(
            value = "$label\n${String.format(Locale.US, "%.1f", smoothedCrest)} dB",
            label = "DYNAMICS",
            confidence = confidence
        )
    }

    override fun reset() {
        smoothedCrest = 0f
        frameCount = 0
    }
}
