package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.*

/**
 * Tuning reference analyzer — detects whether A=440 Hz or offset.
 *
 * Finds the strongest spectral peak near expected A frequencies
 * (55, 110, 220, 440, 880, 1760 Hz) and measures the deviation.
 * Reports the estimated A reference and cents offset.
 *
 * Uses parabolic interpolation on FFT bins for sub-bin frequency accuracy.
 */
class TuningAnalyzer : Analyzer {

    override val id = "tuning"
    override val name = "Tuning"
    override val displayPriority = 32

    private var smoothedReference = 440f
    private var frameCount = 0
    private var validFrameCount = 0

    /** Expected A frequencies across octaves. */
    private val aFrequencies = floatArrayOf(55f, 110f, 220f, 440f, 880f, 1760f)

    /** Search window: +/- 3% around each expected frequency (~50 cents). */
    private val searchRatio = 0.03f

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val spectrum = frame.spectrum
        val binWidth = frame.binWidth

        // Need enough spectral energy
        var totalEnergy = 0.0
        for (v in spectrum) totalEnergy += v
        if (totalEnergy < 1e-8) {
            return if (frameCount < 10) {
                AnalyzerResult.Pending("TUNING", "Listening...")
            } else {
                AnalyzerResult.HeroValue(value = "—", label = "TUNING", confidence = 0f)
            }
        }

        // For each expected A frequency, find the peak in a narrow window
        var bestPeakMag = 0f
        var bestPeakFreq = 440f
        var bestExpected = 440f

        for (expected in aFrequencies) {
            val lowBin = maxOf(1, ((expected * (1 - searchRatio)) / binWidth).toInt())
            val highBin = minOf(spectrum.size - 2, ((expected * (1 + searchRatio)) / binWidth).toInt())

            if (lowBin >= highBin) continue

            // Find peak bin in window
            var peakBin = lowBin
            var peakMag = spectrum[lowBin]
            for (bin in lowBin..highBin) {
                if (spectrum[bin] > peakMag) {
                    peakMag = spectrum[bin]
                    peakBin = bin
                }
            }

            if (peakMag < 1e-10f) continue

            // Parabolic interpolation for sub-bin accuracy
            val alpha = spectrum[maxOf(0, peakBin - 1)]
            val beta = spectrum[peakBin]
            val gamma = spectrum[minOf(spectrum.size - 1, peakBin + 1)]
            val denom = alpha - 2 * beta + gamma
            val delta = if (abs(denom) > 1e-10f) {
                0.5f * (alpha - gamma) / denom
            } else {
                0f
            }
            val interpolatedFreq = (peakBin + delta) * binWidth

            // Weight by magnitude (stronger harmonics are more reliable)
            if (peakMag > bestPeakMag) {
                bestPeakMag = peakMag
                bestPeakFreq = interpolatedFreq
                bestExpected = expected
            }
        }

        if (bestPeakMag < 1e-8f) {
            return if (frameCount < 10) {
                AnalyzerResult.Pending("TUNING", "Listening...")
            } else {
                AnalyzerResult.HeroValue(value = "—", label = "TUNING", confidence = 0f)
            }
        }

        // Calculate A4 reference from the detected frequency
        // If we found a peak near 220 Hz, the A4 reference is peak * 2
        val octaveRatio = 440f / bestExpected
        val estimatedA4 = bestPeakFreq * octaveRatio

        validFrameCount++

        // Smooth slowly — tuning reference is very stable
        smoothedReference = if (validFrameCount <= 1) {
            estimatedA4
        } else {
            smoothedReference * 0.9f + estimatedA4 * 0.1f
        }

        // Cents offset from 440
        val centsOffset = 1200f * log2(smoothedReference / 440f)

        val tuningLabel = when {
            abs(centsOffset) < 3f -> "Standard"
            smoothedReference < 440f -> "Flat"
            else -> "Sharp"
        }

        val confidence = (bestPeakMag / (totalEnergy.toFloat() / spectrum.size * 10f))
            .coerceIn(0f, 1f)

        val centsStr = if (centsOffset >= 0) {
            "+${String.format(Locale.US, "%.0f", centsOffset)}"
        } else {
            String.format(Locale.US, "%.0f", centsOffset)
        }

        return AnalyzerResult.HeroValue(
            value = "$tuningLabel\nA=${String.format(Locale.US, "%.1f", smoothedReference)} Hz (${centsStr}¢)",
            label = "TUNING",
            confidence = confidence
        )
    }

    override fun reset() {
        smoothedReference = 440f
        frameCount = 0
        validFrameCount = 0
    }
}
