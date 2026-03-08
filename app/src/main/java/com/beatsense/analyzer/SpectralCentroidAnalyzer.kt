package com.beatsense.analyzer

/**
 * Spectral centroid — the "center of mass" of the frequency spectrum.
 *
 * A single number in Hz indicating where the spectral weight sits.
 * Low values = dark, warm sound. High values = bright, airy sound.
 *
 * Formula: centroid = Σ(f[i] * mag[i]) / Σ(mag[i])
 * where f[i] is the frequency of bin i and mag[i] is its magnitude.
 */
class SpectralCentroidAnalyzer : Analyzer {

    override val id = "spectral-centroid"
    override val name = "Brightness"
    override val displayPriority = 35

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val spectrum = frame.spectrum
        val binWidth = frame.binWidth

        var weightedSum = 0.0
        var magnitudeSum = 0.0

        for (bin in spectrum.indices) {
            val freq = bin * binWidth
            val mag = spectrum[bin]
            weightedSum += freq * mag
            magnitudeSum += mag
        }

        if (magnitudeSum < 0.0001) {
            return AnalyzerResult.Pending("BRIGHTNESS", "No signal")
        }

        val centroidHz = (weightedSum / magnitudeSum).toFloat()

        return AnalyzerResult.HeroValue(
            value = "%.0f Hz".format(centroidHz),
            label = "BRIGHTNESS",
            confidence = 0f
        )
    }

    override fun reset() {
        // Stateless — each frame is independent
    }
}
