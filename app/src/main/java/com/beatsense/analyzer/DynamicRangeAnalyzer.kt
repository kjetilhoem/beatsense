package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.log10
import kotlin.math.max

/**
 * Dynamic range analyzer — difference between loudest and quietest passages.
 *
 * Tracks peak and trough RMS levels over a sliding window and reports the
 * difference in dB. A large dynamic range means the music breathes —
 * quiet sections contrast with loud ones. A narrow range means heavy
 * compression or consistently loud content.
 *
 * Uses a rolling window of RMS values to find peaks and valleys,
 * ignoring silence (gate at -60 dB RMS).
 */
class DynamicRangeAnalyzer : Analyzer {

    override val id = "dynamic_range"
    override val name = "Dynamic Range"
    override val displayPriority = 55

    private val windowSize = 100 // ~10 seconds at 10 Hz
    private val rmsHistory = FloatArray(windowSize)
    private var historyIndex = 0
    private var historyCount = 0
    private var smoothedRange = 0f

    /** Gate: ignore frames below this RMS (silence). */
    private val silenceGate = 1e-4f // ~ -80 dB

    /** Minimum frames before reporting a meaningful range. */
    private val warmupFrames = 30 // ~3 seconds

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val rms = frame.rms

        // Only accumulate non-silent frames
        if (rms > silenceGate) {
            rmsHistory[historyIndex % windowSize] = rms
            historyIndex++
            historyCount = minOf(historyIndex, windowSize)
        }

        if (historyCount < warmupFrames) {
            return AnalyzerResult.Pending("DYNAMIC RANGE", "Gathering data...")
        }

        // Find peak and trough in current window
        var peak = Float.MIN_VALUE
        var trough = Float.MAX_VALUE
        for (i in 0 until historyCount) {
            val v = rmsHistory[i]
            if (v > peak) peak = v
            if (v < trough) trough = v
        }

        // Avoid division by zero or log(0)
        if (trough < silenceGate) trough = silenceGate
        if (peak < silenceGate) {
            return AnalyzerResult.HeroValue(
                value = "—",
                label = "DYNAMIC RANGE",
                confidence = 0f
            )
        }

        val rangeDb = 20f * log10(peak / trough)

        // Exponential smoothing: 0.85 old + 0.15 new (dynamics evolve slowly)
        smoothedRange = if (smoothedRange == 0f) {
            rangeDb
        } else {
            smoothedRange * 0.85f + rangeDb * 0.15f
        }

        val rangeLabel = when {
            smoothedRange < 4f -> "Flat"
            smoothedRange < 8f -> "Narrow"
            smoothedRange < 14f -> "Moderate"
            smoothedRange < 22f -> "Wide"
            else -> "Cinematic"
        }

        // Confidence increases with more data
        val confidence = (historyCount.toFloat() / windowSize).coerceIn(0f, 1f)

        return AnalyzerResult.HeroValue(
            value = "$rangeLabel\n${String.format(Locale.US, "%.1f", smoothedRange)} dB",
            label = "DYNAMIC RANGE",
            confidence = confidence
        )
    }

    override fun reset() {
        historyIndex = 0
        historyCount = 0
        smoothedRange = 0f
        for (i in rmsHistory.indices) rmsHistory[i] = 0f
    }
}
