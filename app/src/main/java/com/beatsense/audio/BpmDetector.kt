package com.beatsense.audio

import kotlin.math.sqrt

/**
 * Real-time BPM detection using onset detection + autocorrelation.
 *
 * Algorithm:
 * 1. Compute RMS energy of each buffer
 * 2. Detect onsets as energy increases relative to local average
 * 3. Autocorrelate onset signal to find dominant periodicity
 * 4. Convert period to BPM, smooth output for stability
 */
object BpmDetector {

    private const val SAMPLE_RATE = AudioCaptureService.SAMPLE_RATE
    private const val BUFFER_SIZE = AudioCaptureService.BUFFER_SIZE

    // Precise buffers-per-second as float to avoid integer division truncation
    private const val BUFFERS_PER_SECOND = SAMPLE_RATE.toFloat() / BUFFER_SIZE

    // 8 seconds of history for reliable autocorrelation
    private const val HISTORY_SECONDS = 8
    private val historyLength = (HISTORY_SECONDS * BUFFERS_PER_SECOND).toInt()

    // Energy history for onset detection
    private val energyHistory = FloatArray(historyLength)
    private var historyIndex = 0
    private var framesProcessed = 0

    // Onset signal for autocorrelation
    private val onsetSignal = FloatArray(historyLength)

    // Smoothed BPM output
    private var smoothedBpm = 0f
    private var confidence = 0f

    // BPM search range: 60-180 BPM
    // lag = (60 / bpm) * buffersPerSecond
    private val minBpmLag = (60f / 180f * BUFFERS_PER_SECOND).toInt().coerceAtLeast(1)
    private val maxBpmLag = (60f / 60f * BUFFERS_PER_SECOND).toInt()

    // Minimum frames before attempting detection (~3 seconds)
    private val minFramesForDetection = (3f * BUFFERS_PER_SECOND).toInt()

    fun detect(buffer: FloatArray): Float {
        // Step 1: Compute RMS energy
        var sum = 0f
        for (sample in buffer) {
            sum += sample * sample
        }
        val energy = sqrt(sum / buffer.size)

        // Step 2: Store energy
        energyHistory[historyIndex] = energy

        // Step 3: Onset detection — energy increase relative to recent average
        val avgWindow = 6
        var avgEnergy = 0f
        for (i in 1..avgWindow) {
            val idx = (historyIndex - i + historyLength) % historyLength
            avgEnergy += energyHistory[idx]
        }
        avgEnergy /= avgWindow

        // Lower threshold (1.15x) to catch more onsets, especially in compressed music
        val onset = if (avgEnergy > 0.0001f && energy > avgEnergy * 1.15f) {
            energy - avgEnergy
        } else {
            0f
        }
        onsetSignal[historyIndex] = onset

        historyIndex = (historyIndex + 1) % historyLength
        framesProcessed++

        // Step 4: Wait for minimum history before attempting detection
        if (framesProcessed < minFramesForDetection) return 0f

        // Step 5: Autocorrelation — run every 5th frame to save CPU
        if (framesProcessed % 5 == 0) {
            val result = autocorrelateBpm()
            if (result > 0f) {
                smoothedBpm = if (smoothedBpm == 0f) {
                    result
                } else {
                    // If new BPM is close to current, smooth heavily.
                    // If it's very different, adapt faster (tempo change)
                    val diff = kotlin.math.abs(result - smoothedBpm)
                    if (diff < 10f) {
                        smoothedBpm * 0.9f + result * 0.1f
                    } else {
                        smoothedBpm * 0.6f + result * 0.4f
                    }
                }
            }
        }

        return smoothedBpm
    }

    fun getConfidence(): Float = confidence

    private fun autocorrelateBpm(): Float {
        var bestCorrelation = 0f
        var bestLag = 0
        val len = historyLength
        val usableLen = minOf(framesProcessed, len)

        // Normalize onset signal energy for correlation
        var onsetEnergy = 0f
        for (i in 0 until usableLen) {
            val idx = (historyIndex - usableLen + i + len) % len
            onsetEnergy += onsetSignal[idx] * onsetSignal[idx]
        }
        if (onsetEnergy < 0.00001f) {
            confidence = 0f
            return 0f
        }

        for (lag in minBpmLag..maxBpmLag.coerceAtMost(usableLen / 2)) {
            var correlation = 0f
            val count = usableLen - lag
            for (i in 0 until count) {
                val idx1 = (historyIndex - usableLen + i + len) % len
                val idx2 = (historyIndex - usableLen + i + lag + len) % len
                correlation += onsetSignal[idx1] * onsetSignal[idx2]
            }
            correlation /= count

            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestLag = lag
            }
        }

        if (bestLag == 0) {
            confidence = 0f
            return 0f
        }

        // Confidence based on correlation strength relative to onset energy
        confidence = (bestCorrelation / (onsetEnergy / usableLen + 0.00001f)).coerceIn(0f, 1f)

        // Convert lag (in buffers) to BPM
        val secondsPerBeat = bestLag.toFloat() / BUFFERS_PER_SECOND
        val bpm = 60f / secondsPerBeat

        // Validate BPM is in reasonable range
        return if (bpm in 55f..185f) bpm else 0f
    }

    fun reset() {
        energyHistory.fill(0f)
        onsetSignal.fill(0f)
        historyIndex = 0
        framesProcessed = 0
        smoothedBpm = 0f
        confidence = 0f
    }
}
