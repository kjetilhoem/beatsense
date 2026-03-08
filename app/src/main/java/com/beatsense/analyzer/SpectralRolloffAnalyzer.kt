package com.beatsense.analyzer

import java.util.Locale

/**
 * Spectral rolloff analyzer — frequency below which N% of spectral energy sits.
 *
 * Rolloff at 85% is a standard measure of spectral shape. A low rolloff
 * means energy is concentrated in bass frequencies (dark/warm). A high
 * rolloff means significant energy in the treble range (bright/airy).
 *
 * Complementary to spectral centroid: centroid is the balance point,
 * rolloff is the "ceiling" where most energy lives.
 */
class SpectralRolloffAnalyzer : Analyzer {

    override val id = "spectral_rolloff"
    override val name = "Rolloff"
    override val displayPriority = 36

    private var smoothedRolloff = 0f
    private var frameCount = 0

    /** Fraction of total energy used as the rolloff threshold. */
    private val rolloffFraction = 0.85f

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val spectrum = frame.spectrum
        var totalEnergy = 0.0
        for (bin in spectrum.indices) {
            totalEnergy += spectrum[bin]
        }

        if (totalEnergy < 1e-10) {
            return if (frameCount < 5) {
                AnalyzerResult.Pending("ROLLOFF", "Listening...")
            } else {
                AnalyzerResult.HeroValue(
                    value = "—",
                    label = "ROLLOFF",
                    confidence = 0f
                )
            }
        }

        val threshold = totalEnergy * rolloffFraction
        var cumulativeEnergy = 0.0
        var rolloffBin = spectrum.size - 1

        for (bin in spectrum.indices) {
            cumulativeEnergy += spectrum[bin]
            if (cumulativeEnergy >= threshold) {
                rolloffBin = bin
                break
            }
        }

        val rolloffFreq = rolloffBin * frame.binWidth

        // Exponential smoothing: 0.7 old + 0.3 new
        smoothedRolloff = if (smoothedRolloff == 0f) {
            rolloffFreq
        } else {
            smoothedRolloff * 0.7f + rolloffFreq * 0.3f
        }

        val shapeLabel = when {
            smoothedRolloff < 500f -> "Dark"
            smoothedRolloff < 2000f -> "Warm"
            smoothedRolloff < 5000f -> "Balanced"
            smoothedRolloff < 10000f -> "Bright"
            else -> "Airy"
        }

        val confidence = (totalEnergy / (spectrum.size * 0.01)).toFloat().coerceIn(0f, 1f)

        return AnalyzerResult.HeroValue(
            value = "$shapeLabel\n${String.format(Locale.US, "%.0f", smoothedRolloff)} Hz",
            label = "ROLLOFF",
            confidence = confidence
        )
    }

    override fun reset() {
        smoothedRolloff = 0f
        frameCount = 0
    }
}
