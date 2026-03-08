package com.beatsense.audio

import kotlin.math.*

/**
 * Musical key detection using chromagram + Krumhansl-Schmuckler algorithm.
 *
 * Psychoacoustic constraints applied:
 * - A single note cannot establish a key (Krumhansl & Kessler, 1982)
 * - Requires ≥3 distinct pitch classes with clear tonal hierarchy
 * - Flat chromagram (all pitch classes roughly equal) = no tonal center
 * - Insufficient energy or signal-to-noise = no report
 * - "If a trained musician couldn't tell the key, the algorithm says '—'"
 */
object KeyDetector {

    private const val SAMPLE_RATE = AudioConfig.SAMPLE_RATE
    private const val FFT_SIZE = AudioConfig.BUFFER_SIZE

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Krumhansl-Schmuckler key profiles
    private val MAJOR_PROFILE = floatArrayOf(6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f, 2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f)
    private val MINOR_PROFILE = floatArrayOf(6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f, 2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f)

    // Long-term accumulated chromagram — slow decay for overall key
    private val chromaLong = FloatArray(12)

    private var frameCount = 0
    private var lastKey: String? = null
    private var lastRoot: String? = null
    private var lastMode: String? = null
    private var confidence = 0f

    /**
     * Separated key detection result.
     * Root and mode are independent observations — root can stay stable
     * while mode flips between Major/Minor based on new evidence.
     */
    data class KeyResult(
        val root: String?,
        val mode: String?,
        val confidence: Float
    ) {
        val combined: String? get() = if (root != null && mode != null) "$root $mode" else null
    }

    // Pre-computed constants
    private val binWidth = SAMPLE_RATE.toFloat() / FFT_SIZE
    private val minBin = (55.0 / binWidth).toInt()     // A1 ~55Hz
    private val maxBin = minOf((2000.0 / binWidth).toInt(), FFT_SIZE / 2)

    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        0.5f * (1f - cos(2f * PI.toFloat() * i / (FFT_SIZE - 1)))
    }

    private val binPitchClass = IntArray(maxBin + 1) { bin ->
        val freq = bin * binWidth
        if (freq > 20f) {
            val midiNote = 12f * log2(freq / 440f) + 69f
            ((midiNote.roundToInt() % 12) + 12) % 12
        } else -1
    }

    fun detect(buffer: FloatArray): String? {
        // Step 1: Apply Hann window
        val windowed = FloatArray(FFT_SIZE)
        val len = minOf(buffer.size, FFT_SIZE)
        for (i in 0 until len) {
            windowed[i] = buffer[i] * hannWindow[i]
        }

        // Step 2: Compute chroma from DFT
        val currentChroma = FloatArray(12)
        for (bin in minBin..maxBin) {
            var real = 0f
            var imag = 0f
            val angleStep = 2f * PI.toFloat() * bin / FFT_SIZE
            for (n in 0 until len) {
                val angle = angleStep * n
                real += windowed[n] * cos(angle)
                imag += windowed[n] * sin(angle)
            }
            val energy = real * real + imag * imag
            val pc = binPitchClass[bin]
            if (pc >= 0) {
                currentChroma[pc] += energy
            }
        }

        // Step 3: Accumulate with slow decay (~10+ seconds effective window)
        for (i in 0 until 12) {
            chromaLong[i] = chromaLong[i] * 0.97f + currentChroma[i] * 0.03f
        }

        frameCount++

        // Wait for substantial accumulation (~5 seconds)
        if (frameCount < 50) return lastKey

        // Only re-evaluate every ~1 second for stability
        if (frameCount % 10 != 0) return lastKey

        // Step 4: Check if there's sufficient tonal evidence before attempting key detection
        val evidence = assessTonalEvidence(chromaLong)

        if (!evidence.sufficient) {
            // Not enough harmonic evidence — don't guess
            confidence = 0f
            lastKey = "—"
            return lastKey
        }

        // Step 5: Key detection on validated chroma
        val result = detectKey(chromaLong)
        confidence = result.confidence

        // Final gate: if confidence is too low even after detection, don't report
        if (confidence < 0.08f) {
            lastKey = "—"
            lastRoot = null
            lastMode = null
            confidence = 0f
        } else {
            lastRoot = result.root
            lastMode = result.mode
            lastKey = "${result.root} ${result.mode}"
        }

        return lastKey
    }

    /**
     * Returns root and mode as independent values.
     * Call after detect() for the current frame's result.
     */
    fun getKeyResult(): KeyResult = KeyResult(lastRoot, lastMode, confidence)

    /**
     * Assess whether the chromagram contains sufficient tonal evidence to determine a key.
     *
     * Based on psychoacoustic research:
     * - A single note doesn't establish a key (Krumhansl & Kessler, 1982)
     * - Need ≥3 distinct active pitch classes
     * - Chromagram must show clear peaks, not be flat (which indicates noise or silence)
     */
    private data class TonalEvidence(
        val sufficient: Boolean,
        val activePitchClasses: Int,
        val peakiness: Float    // how much the chroma deviates from uniform — 0 = flat, higher = peaky
    )

    private fun assessTonalEvidence(chroma: FloatArray): TonalEvidence {
        val maxVal = chroma.max()
        if (maxVal < 0.00001f) {
            return TonalEvidence(sufficient = false, activePitchClasses = 0, peakiness = 0f)
        }

        // Normalize to 0-1
        val normalized = FloatArray(12) { chroma[it] / maxVal }

        // Count "active" pitch classes: significantly above the noise floor
        // A pitch class is active if it has at least 20% of the max energy
        val activeThreshold = 0.20f
        val activePitchClasses = normalized.count { it >= activeThreshold }

        // Compute "peakiness" — how far the distribution is from uniform
        // Uniform distribution (noise) has equal energy in all 12 bins
        // Use coefficient of variation: std / mean
        val mean = normalized.average().toFloat()
        var variance = 0f
        for (v in normalized) {
            val diff = v - mean
            variance += diff * diff
        }
        variance /= 12f
        val std = sqrt(variance)
        val peakiness = if (mean > 0.001f) std / mean else 0f

        // Sufficient evidence requires:
        // 1. At least 3 active pitch classes (single note or interval can't define a key)
        // 2. Chromagram is not too flat (peakiness > threshold)
        //    Peakiness of ~0.3 means some differentiation. >0.5 means clear tonal structure
        //    Pure noise ≈ 0.1-0.2. Single strong note ≈ 0.8-1.0 (but fails pitch class count)
        val sufficient = activePitchClasses >= 3 && peakiness > 0.35f

        return TonalEvidence(
            sufficient = sufficient,
            activePitchClasses = activePitchClasses,
            peakiness = peakiness
        )
    }

    private data class DetectionResult(
        val root: String,
        val mode: String,
        val confidence: Float
    )

    private fun detectKey(chroma: FloatArray): DetectionResult {
        val maxVal = chroma.max()
        if (maxVal < 0.00001f) return DetectionResult("—", "", 0f)

        val normalized = FloatArray(12) { chroma[it] / maxVal }

        var bestCorrelation = -Float.MAX_VALUE
        var secondBest = -Float.MAX_VALUE
        var bestRoot = "—"
        var bestMode = ""

        for (root in 0 until 12) {
            val rotated = FloatArray(12) { normalized[(it + root) % 12] }

            val majorCorr = pearsonCorrelation(rotated, MAJOR_PROFILE)
            if (majorCorr > bestCorrelation) {
                secondBest = bestCorrelation
                bestCorrelation = majorCorr
                bestRoot = NOTE_NAMES[root]
                bestMode = "Major"
            } else if (majorCorr > secondBest) {
                secondBest = majorCorr
            }

            val minorCorr = pearsonCorrelation(rotated, MINOR_PROFILE)
            if (minorCorr > bestCorrelation) {
                secondBest = bestCorrelation
                bestCorrelation = minorCorr
                bestRoot = NOTE_NAMES[root]
                bestMode = "Minor"
            } else if (minorCorr > secondBest) {
                secondBest = minorCorr
            }
        }

        // Confidence: gap between best and second-best, normalized
        val gap = bestCorrelation - secondBest
        val conf = (gap / bestCorrelation.coerceAtLeast(0.001f)).coerceIn(0f, 1f)

        return DetectionResult(bestRoot, bestMode, conf)
    }

    fun getConfidence(): Float = confidence

    private fun pearsonCorrelation(x: FloatArray, y: FloatArray): Float {
        val n = x.size
        var sumX = 0f; var sumY = 0f
        var sumXY = 0f; var sumX2 = 0f; var sumY2 = 0f

        for (i in 0 until n) {
            sumX += x[i]; sumY += y[i]
            sumXY += x[i] * y[i]
            sumX2 += x[i] * x[i]
            sumY2 += y[i] * y[i]
        }

        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
        return if (denominator < 0.00001f) 0f else numerator / denominator
    }

    fun reset() {
        chromaLong.fill(0f)
        frameCount = 0
        lastKey = null
        lastRoot = null
        lastMode = null
        confidence = 0f
    }
}
