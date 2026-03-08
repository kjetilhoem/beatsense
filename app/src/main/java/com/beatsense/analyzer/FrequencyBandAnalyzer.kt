package com.beatsense.analyzer

/**
 * Frequency band energy analyzer — energy distribution across 5 perceptual bands.
 *
 * Bands follow standard audio engineering divisions:
 * - Sub-bass  (<60 Hz)  — felt more than heard, kick drum fundamentals
 * - Bass      (60-250 Hz) — bass guitar, warmth
 * - Mids      (250-4000 Hz) — vocals, most instruments
 * - Presence  (4-8 kHz) — clarity, consonants, attack
 * - Air       (8 kHz+) — sparkle, breath, cymbal shimmer
 *
 * Uses pre-computed spectrum from AudioFrame. Applies per-band
 * smoothing for stable visual display.
 */
class FrequencyBandAnalyzer : Analyzer {

    override val id = "frequency_bands"
    override val name = "Spectrum"
    override val displayPriority = 30

    private val smoothedBands = FloatArray(5)

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val spectrum = frame.spectrum
        val binWidth = frame.binWidth
        val bandEnergies = FloatArray(5)

        for (bin in spectrum.indices) {
            val freq = bin * binWidth
            val bandIndex = when {
                freq < 60f -> 0    // Sub-bass
                freq < 250f -> 1   // Bass
                freq < 4000f -> 2  // Mids
                freq < 8000f -> 3  // Presence
                else -> 4          // Air
            }
            bandEnergies[bandIndex] += spectrum[bin]
        }

        // Normalize: find the max energy across bands for relative display
        val maxEnergy = bandEnergies.max()
        val normalizedBands = if (maxEnergy > 0f) {
            FloatArray(5) { (bandEnergies[it] / maxEnergy).coerceIn(0f, 1f) }
        } else {
            FloatArray(5)
        }

        // Smooth each band independently (0.6 old + 0.4 new)
        for (i in smoothedBands.indices) {
            smoothedBands[i] = if (smoothedBands[i] == 0f && normalizedBands[i] > 0f) {
                normalizedBands[i]
            } else {
                smoothedBands[i] * 0.6f + normalizedBands[i] * 0.4f
            }
        }

        return AnalyzerResult.Bands(
            label = "SPECTRUM",
            bands = listOf(
                AnalyzerResult.Bands.Band("Sub", smoothedBands[0]),
                AnalyzerResult.Bands.Band("Bass", smoothedBands[1]),
                AnalyzerResult.Bands.Band("Mids", smoothedBands[2]),
                AnalyzerResult.Bands.Band("Pres", smoothedBands[3]),
                AnalyzerResult.Bands.Band("Air", smoothedBands[4])
            )
        )
    }

    override fun reset() {
        smoothedBands.fill(0f)
    }
}
