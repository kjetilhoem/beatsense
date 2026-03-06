package com.beatsense.audio

import kotlin.math.sqrt

/**
 * Computes audio level (RMS) for visualization.
 * Provides a smoothed 0-1 value suitable for UI meters.
 */
object AudioAnalyzer {

    private var smoothedLevel = 0f

    fun computeLevel(buffer: FloatArray): Float {
        var sum = 0f
        for (sample in buffer) {
            sum += sample * sample
        }
        val rms = sqrt(sum / buffer.size)

        // Map RMS to 0-1 range (typical music RMS: 0.01 - 0.3)
        val normalized = (rms / 0.25f).coerceIn(0f, 1f)

        // Smooth: fast attack, slow release (like a VU meter)
        smoothedLevel = if (normalized > smoothedLevel) {
            smoothedLevel * 0.3f + normalized * 0.7f  // fast attack
        } else {
            smoothedLevel * 0.92f + normalized * 0.08f // slow release
        }

        return smoothedLevel
    }

    fun reset() {
        smoothedLevel = 0f
    }
}
