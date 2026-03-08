package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.log10

/**
 * LUFS metering — integrated loudness per EBU R128.
 *
 * Reports three loudness measurements:
 * - LUFS-M (Momentary): 400ms sliding window — instant loudness
 * - LUFS-S (Short-term): 3s sliding window — phrase-level loudness
 * - LUFS-I (Integrated): running average since start — overall loudness
 *
 * Simplified mono implementation: uses mean square energy directly.
 * Full EBU R128 requires K-weighting (two-stage biquad filter) but
 * unweighted mean square is sufficient for real-time display and
 * relative comparisons.
 */
class LufsAnalyzer : Analyzer {

    override val id = "lufs"
    override val name = "Loudness"
    override val displayPriority = 25

    companion object {
        private const val FRAMES_PER_SECOND = 44100f / 4096f  // ~10.77
        private val MOMENTARY_FRAMES = (0.4f * FRAMES_PER_SECOND).toInt().coerceAtLeast(1)  // ~4 frames
        private val SHORT_TERM_FRAMES = (3f * FRAMES_PER_SECOND).toInt()  // ~32 frames
        private const val SILENCE_GATE = -70f  // LUFS below this is silence
    }

    // Circular buffers for mean square energy
    private val momentaryBuffer = FloatArray(MOMENTARY_FRAMES)
    private var momentaryIndex = 0
    private val shortTermBuffer = FloatArray(SHORT_TERM_FRAMES)
    private var shortTermIndex = 0

    // Integrated loudness: running sum
    private var integratedSum = 0.0
    private var integratedCount = 0L
    private var frameCount = 0

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        // Compute mean square energy (unweighted)
        var meanSquare = 0.0
        for (sample in frame.pcm) {
            meanSquare += (sample * sample).toDouble()
        }
        meanSquare /= frame.pcm.size

        // Store in circular buffers
        momentaryBuffer[momentaryIndex % MOMENTARY_FRAMES] = meanSquare.toFloat()
        momentaryIndex++
        shortTermBuffer[shortTermIndex % SHORT_TERM_FRAMES] = meanSquare.toFloat()
        shortTermIndex++

        // Accumulate for integrated measurement (gate silence)
        val frameLufs = meanSquareToLufs(meanSquare)
        if (frameLufs > SILENCE_GATE) {
            integratedSum += meanSquare
            integratedCount++
        }

        if (frameCount < MOMENTARY_FRAMES) {
            return AnalyzerResult.Pending("LOUDNESS", "Measuring...")
        }

        // Compute momentary LUFS (400ms window)
        val momentaryMs = averageBuffer(momentaryBuffer,
            minOf(momentaryIndex, MOMENTARY_FRAMES))
        val lufsM = meanSquareToLufs(momentaryMs)

        // Compute short-term LUFS (3s window)
        val shortTermMs = averageBuffer(shortTermBuffer,
            minOf(shortTermIndex, SHORT_TERM_FRAMES))
        val lufsS = meanSquareToLufs(shortTermMs)

        // Compute integrated LUFS
        val lufsI = if (integratedCount > 0) {
            meanSquareToLufs(integratedSum / integratedCount)
        } else {
            SILENCE_GATE
        }

        return AnalyzerResult.ValueGroup(
            label = "LOUDNESS",
            values = listOf(
                AnalyzerResult.ValueGroup.LabeledValue("M", formatLufs(lufsM), "LUFS"),
                AnalyzerResult.ValueGroup.LabeledValue("S", formatLufs(lufsS), "LUFS"),
                AnalyzerResult.ValueGroup.LabeledValue("I", formatLufs(lufsI), "LUFS")
            )
        )
    }

    private fun meanSquareToLufs(ms: Double): Float {
        return if (ms > 1e-10) {
            (-0.691f + 10f * log10(ms.toFloat()))
        } else {
            -70f
        }
    }

    private fun averageBuffer(buffer: FloatArray, count: Int): Double {
        var sum = 0.0
        val n = minOf(count, buffer.size)
        for (i in 0 until n) {
            sum += buffer[i]
        }
        return if (n > 0) sum / n else 0.0
    }

    private fun formatLufs(lufs: Float): String {
        return if (lufs <= SILENCE_GATE) "—" else String.format(Locale.US, "%.1f", lufs)
    }

    override fun reset() {
        momentaryBuffer.fill(0f)
        momentaryIndex = 0
        shortTermBuffer.fill(0f)
        shortTermIndex = 0
        integratedSum = 0.0
        integratedCount = 0
        frameCount = 0
    }
}
