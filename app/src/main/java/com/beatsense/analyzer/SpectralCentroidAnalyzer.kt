package com.beatsense.analyzer

import java.util.Locale

/**
 * Spectral centroid analyzer — where the spectral weight sits.
 *
 * The centroid is the "center of mass" of the spectrum, expressed in Hz.
 * Low centroid = dark/warm sound, high centroid = bright/harsh sound.
 * Useful for distinguishing bass-heavy vs. treble-heavy content.
 *
 * Uses pre-computed spectrum from AudioFrame. Applies exponential
 * smoothing to avoid jitter (audio thread at ~10 Hz).
 */
class SpectralCentroidAnalyzer : Analyzer {

    override val id = "spectral_centroid"
    override val name = "Brightness"
    override val displayPriority = 35

    private var smoothedCentroid = 0f
    private var frameCount = 0

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val spectrum = frame.spectrum
        var weightedSum = 0.0
        var magnitudeSum = 0.0

        for (bin in spectrum.indices) {
            val freq = bin * frame.binWidth
            weightedSum += freq * spectrum[bin]
            magnitudeSum += spectrum[bin]
        }

        if (magnitudeSum < 1e-10) {
            return if (frameCount < 5) {
                AnalyzerResult.Pending("BRIGHTNESS", "Listening...")
            } else {
                AnalyzerResult.HeroValue(
                    value = "—",
                    label = "BRIGHTNESS",
                    confidence = 0f
                )
            }
        }

        val centroid = (weightedSum / magnitudeSum).toFloat()

        // Exponential smoothing: 0.7 old + 0.3 new
        smoothedCentroid = if (smoothedCentroid == 0f) {
            centroid
        } else {
            smoothedCentroid * 0.7f + centroid * 0.3f
        }

        // Map centroid to a qualitative brightness label
        val brightnessLabel = when {
            smoothedCentroid < 500f -> "Dark"
            smoothedCentroid < 1500f -> "Warm"
            smoothedCentroid < 3000f -> "Balanced"
            smoothedCentroid < 6000f -> "Bright"
            else -> "Harsh"
        }

        // Confidence based on spectral energy — low energy = low confidence
        val confidence = (magnitudeSum / (spectrum.size * 0.01)).toFloat().coerceIn(0f, 1f)

        return AnalyzerResult.HeroValue(
            value = "$brightnessLabel\n${String.format(Locale.US, "%.0f", smoothedCentroid)} Hz",
            label = "BRIGHTNESS",
            confidence = confidence
        )
    }

    override fun reset() {
        smoothedCentroid = 0f
        frameCount = 0
    }
}
