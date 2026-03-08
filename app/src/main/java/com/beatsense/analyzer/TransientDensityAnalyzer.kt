package com.beatsense.analyzer

/**
 * Transient density analyzer — how "busy" the rhythm is.
 *
 * Counts detected onsets (energy increases above local average) per second.
 * Low density: ambient, sustained pads. High density: drum fills, fast strumming.
 *
 * Maintains a circular buffer of per-frame onset flags and computes
 * density over a sliding window.
 */
class TransientDensityAnalyzer : Analyzer {

    override val id = "transient_density"
    override val name = "Transients"
    override val displayPriority = 40

    companion object {
        private const val HISTORY_SECONDS = 4
        private const val ENERGY_HISTORY_SIZE = 6  // frames for local average
        private const val ONSET_THRESHOLD = 1.15f  // energy must exceed average by 15%
    }

    // Circular buffer of energy values for onset detection
    private val energyHistory = FloatArray(ENERGY_HISTORY_SIZE)
    private var energyIndex = 0

    // Circular buffer tracking onset flags over the window
    private val windowSize = (HISTORY_SECONDS * 44100f / 4096f).toInt()  // ~43 frames
    private val onsetFlags = BooleanArray(windowSize)
    private var windowIndex = 0

    private var frameCount = 0

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        frameCount++
        val energy = frame.rms

        // Compute local average energy
        var avgEnergy = 0f
        for (e in energyHistory) avgEnergy += e
        avgEnergy /= ENERGY_HISTORY_SIZE

        // Detect onset
        val isOnset = energy > avgEnergy * ONSET_THRESHOLD && energy > 0.01f

        // Store energy in history
        energyHistory[energyIndex % ENERGY_HISTORY_SIZE] = energy
        energyIndex++

        // Store onset flag in sliding window
        onsetFlags[windowIndex % windowSize] = isOnset
        windowIndex++

        if (frameCount < windowSize / 2) {
            return AnalyzerResult.Pending("TRANSIENTS", "Accumulating...")
        }

        // Count onsets in window
        var onsetCount = 0
        for (flag in onsetFlags) {
            if (flag) onsetCount++
        }

        // Convert to onsets per second
        val framesPerSecond = 44100f / 4096f  // ~10.77
        val windowDurationSeconds = windowSize / framesPerSecond
        val onsetsPerSecond = onsetCount / windowDurationSeconds

        val densityLabel = when {
            onsetsPerSecond < 1f -> "Sparse"
            onsetsPerSecond < 3f -> "Relaxed"
            onsetsPerSecond < 6f -> "Moderate"
            onsetsPerSecond < 10f -> "Busy"
            else -> "Dense"
        }

        return AnalyzerResult.HeroValue(
            value = "$densityLabel\n${String.format(java.util.Locale.US, "%.1f", onsetsPerSecond)}/s",
            label = "TRANSIENTS",
            confidence = (onsetsPerSecond / 12f).coerceIn(0f, 1f)
        )
    }

    override fun reset() {
        energyHistory.fill(0f)
        energyIndex = 0
        onsetFlags.fill(false)
        windowIndex = 0
        frameCount = 0
    }
}
