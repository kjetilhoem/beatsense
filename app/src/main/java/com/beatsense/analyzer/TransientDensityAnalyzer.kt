package com.beatsense.analyzer

import java.util.Locale
import kotlin.math.abs

/**
 * Transient density analyzer — onsets per second.
 *
 * Counts how many transients (sharp energy increases) occur per second.
 * Low density = sparse, ambient. High density = busy, percussive.
 *
 * Uses a simple energy-rise onset detector: compare current frame energy
 * to a moving average, flag when the ratio exceeds a threshold.
 */
class TransientDensityAnalyzer : Analyzer {

    override val id = "transient_density"
    override val name = "Transient Density"
    override val displayPriority = 45

    private var smoothedDensity = 0f
    private var frameCount = 0
    private var movingEnergy = 0f
    private val onsetTimes = mutableListOf<Int>() // frame indices of detected onsets
    private val onsetThreshold = 2.0f // energy must be 2x the moving average

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++

        val energy = frame.rms * frame.rms

        // Update moving average
        movingEnergy = if (movingEnergy == 0f) {
            energy
        } else {
            movingEnergy * 0.95f + energy * 0.05f
        }

        // Onset detection
        if (movingEnergy > 1e-10f && energy / movingEnergy > onsetThreshold) {
            // Only count if enough time since last onset (debounce ~50ms = ~0.5 frames at 10Hz)
            val lastOnset = onsetTimes.lastOrNull() ?: -10
            if (frameCount - lastOnset >= 1) {
                onsetTimes.add(frameCount)
            }
        }

        // Remove onsets older than ~3 seconds (30 frames at 10Hz)
        val windowFrames = 30
        onsetTimes.removeAll { it < frameCount - windowFrames }

        if (frameCount < 10) {
            return AnalyzerResult.Pending("TRANSIENTS", "Listening...")
        }

        // Calculate onsets per second
        val windowSeconds = minOf(frameCount, windowFrames) / 10f
        val onsetsPerSecond = if (windowSeconds > 0) {
            onsetTimes.size / windowSeconds
        } else {
            0f
        }

        // Smooth
        smoothedDensity = if (smoothedDensity == 0f) {
            onsetsPerSecond
        } else {
            smoothedDensity * 0.7f + onsetsPerSecond * 0.3f
        }

        val label = when {
            smoothedDensity < 1f -> "Sparse"
            smoothedDensity < 3f -> "Moderate"
            smoothedDensity < 6f -> "Active"
            else -> "Dense"
        }

        val confidence = (movingEnergy / 0.001f).coerceIn(0f, 1f)

        return AnalyzerResult.HeroValue(
            value = "$label\n${String.format(Locale.US, "%.1f", smoothedDensity)}/s",
            label = "TRANSIENTS",
            confidence = confidence
        )
    }

    override fun reset() {
        smoothedDensity = 0f
        frameCount = 0
        movingEnergy = 0f
        onsetTimes.clear()
    }
}
