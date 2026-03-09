package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.log10
import kotlin.math.max

/**
 * Dynamic range analyzer — peak-to-trough RMS over a sliding window.
 *
 * Tracks loud and quiet passages over ~10 seconds and reports the
 * difference in dB. Silence is gated out.
 */
class DynamicRangeAnalyzer : Analyzer {

    override val id = "dynamic_range"
    override val name = "Dynamic Range"
    override val displayPriority = 55

    private val windowSize = 100
    private val rmsHistory = FloatArray(windowSize)
    private var historyIndex = 0
    private var historyCount = 0
    private var smoothedRange = 0f
    private val silenceGate = 1e-4f
    private val warmupFrames = 30

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        val rms = frame.rms

        if (rms > silenceGate) {
            rmsHistory[historyIndex % windowSize] = rms
            historyIndex++
            historyCount = minOf(historyIndex, windowSize)
        }

        if (historyCount < warmupFrames) {
            return AnalyzerResult.Pending("DYNAMIC RANGE", "Gathering data...")
        }

        var peak = Float.MIN_VALUE
        var trough = Float.MAX_VALUE
        for (i in 0 until historyCount) {
            val v = rmsHistory[i]
            if (v > peak) peak = v
            if (v < trough) trough = v
        }

        if (trough < silenceGate) trough = silenceGate
        if (peak < silenceGate) {
            return AnalyzerResult.HeroValue(value = "—", label = "DYNAMIC RANGE", confidence = 0f)
        }

        val rangeDb = 20f * log10(peak / trough)

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
