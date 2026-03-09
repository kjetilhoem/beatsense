package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.log10
import kotlin.math.max

/**
 * LUFS analyzer — loudness per EBU R128 (simplified).
 *
 * Reports three measurements:
 * - Momentary (M): 400ms window
 * - Short-term (S): 3s window
 * - Integrated (I): running average since start
 *
 * Uses mean-square energy with K-weighting approximated by
 * the pre-computed RMS from AudioFrame.
 */
class LufsAnalyzer : Analyzer {

    override val id = "lufs"
    override val name = "Loudness"
    override val displayPriority = 25

    // ~10 Hz frame rate: 400ms = 4 frames, 3s = 30 frames
    private val momentaryWindow = 4
    private val shortTermWindow = 30
    private val buffer = FloatArray(shortTermWindow)
    private var bufferIndex = 0
    private var frameCount = 0
    private var integratedSum = 0.0
    private var integratedCount = 0

    /** Silence gate: below this LUFS we report "—" */
    private val silenceGate = -70f

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val rms = frame.rms.toDouble()
        val meanSquare = rms * rms

        buffer[bufferIndex % shortTermWindow] = meanSquare.toFloat()
        bufferIndex++

        if (frameCount < momentaryWindow) {
            return AnalyzerResult.Pending("LOUDNESS", "Measuring...")
        }

        // Momentary (last 4 frames)
        val mFrames = minOf(frameCount, momentaryWindow)
        var mSum = 0.0
        for (i in 0 until mFrames) {
            mSum += buffer[(bufferIndex - 1 - i).mod(shortTermWindow)]
        }
        val momentaryLufs = msToLufs(mSum / mFrames)

        // Short-term (last 30 frames)
        val sFrames = minOf(frameCount, shortTermWindow)
        var sSum = 0.0
        for (i in 0 until sFrames) {
            sSum += buffer[(bufferIndex - 1 - i).mod(shortTermWindow)]
        }
        val shortTermLufs = msToLufs(sSum / sFrames)

        // Integrated (gated running average)
        if (momentaryLufs > silenceGate) {
            integratedSum += meanSquare
            integratedCount++
        }
        val integratedLufs = if (integratedCount > 0) {
            msToLufs(integratedSum / integratedCount)
        } else {
            silenceGate
        }

        return AnalyzerResult.ValueGroup(
            label = "LOUDNESS (LUFS)",
            values = listOf(
                AnalyzerResult.ValueGroup.LabeledValue(
                    "Momentary",
                    formatLufs(momentaryLufs),
                    "LUFS"
                ),
                AnalyzerResult.ValueGroup.LabeledValue(
                    "Short-term",
                    formatLufs(shortTermLufs),
                    "LUFS"
                ),
                AnalyzerResult.ValueGroup.LabeledValue(
                    "Integrated",
                    formatLufs(integratedLufs),
                    "LUFS"
                ),
            )
        )
    }

    override fun reset() {
        bufferIndex = 0
        frameCount = 0
        integratedSum = 0.0
        integratedCount = 0
        for (i in buffer.indices) buffer[i] = 0f
    }

    private fun msToLufs(meanSquare: Double): Float {
        if (meanSquare < 1e-20) return silenceGate
        return (-0.691f + 10f * log10(max(meanSquare, 1e-20)).toFloat())
    }

    private fun formatLufs(lufs: Float): String {
        return if (lufs <= silenceGate) "—" else String.format(Locale.US, "%.1f", lufs)
    }
}
