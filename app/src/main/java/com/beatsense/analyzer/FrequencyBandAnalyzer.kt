package com.beatsense.analyzer

/**
 * Frequency band energy analyzer — splits the spectrum into 5 bands
 * and reports normalized energy levels for each.
 *
 * Bands:
 *   Sub-bass  : < 60 Hz
 *   Bass      : 60 – 250 Hz
 *   Mids      : 250 Hz – 4 kHz
 *   Presence  : 4 – 8 kHz
 *   Air       : 8 kHz+
 */
class FrequencyBandAnalyzer : Analyzer {

    override val id = "bands"
    override val name = "Frequency Bands"
    override val displayPriority = 30

    private data class BandDef(val name: String, val lowHz: Float, val highHz: Float)

    private val bandDefs = listOf(
        BandDef("Sub", 0f, 60f),
        BandDef("Bass", 60f, 250f),
        BandDef("Mids", 250f, 4000f),
        BandDef("Presence", 4000f, 8000f),
        BandDef("Air", 8000f, Float.MAX_VALUE)
    )

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val spectrum = frame.spectrum
        val binWidth = frame.binWidth
        val numBins = spectrum.size

        // Accumulate energy per band
        val energies = FloatArray(bandDefs.size)
        for (bin in 0 until numBins) {
            val freq = bin * binWidth
            for (i in bandDefs.indices) {
                if (freq >= bandDefs[i].lowHz && freq < bandDefs[i].highHz) {
                    energies[i] += spectrum[bin]
                    break
                }
            }
        }

        // Normalize: map to 0.0–1.0 relative to the max band energy
        val maxEnergy = energies.maxOrNull() ?: 0f
        val bands = bandDefs.mapIndexed { i, def ->
            val level = if (maxEnergy > 0f) energies[i] / maxEnergy else 0f
            AnalyzerResult.Bands.Band(name = def.name, level = level)
        }

        return AnalyzerResult.Bands(label = "BANDS", bands = bands)
    }

    override fun reset() {
        // Stateless — nothing to reset
    }
}
