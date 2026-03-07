package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import kotlin.math.*

/**
 * Pre-computed audio data shared across all analyzers.
 *
 * Like a graphics engine's per-fragment data — expensive computations
 * (RMS, FFT, chromagram) are done once and passed to every analyzer.
 */
class AudioFrame(
    val pcm: FloatArray,
    val sampleRate: Int = AudioConfig.SAMPLE_RATE,
    val bufferSize: Int = AudioConfig.BUFFER_SIZE
) {
    /** RMS energy of the buffer */
    val rms: Float by lazy { computeRms() }

    /** FFT magnitude spectrum (bins 0 to bufferSize/2) */
    val spectrum: FloatArray by lazy { computeSpectrum() }

    /** 12-bin chromagram (C through B) */
    val chromagram: FloatArray by lazy { computeChromagram() }

    /** Frequency resolution: Hz per bin */
    val binWidth: Float = sampleRate.toFloat() / bufferSize

    private fun computeRms(): Float {
        var sum = 0f
        for (sample in pcm) {
            sum += sample * sample
        }
        return sqrt(sum / pcm.size)
    }

    private fun computeSpectrum(): FloatArray {
        val n = minOf(pcm.size, bufferSize)
        val numBins = bufferSize / 2 + 1
        val magnitudes = FloatArray(numBins)

        // Apply Hann window and compute DFT magnitudes
        for (bin in 0 until numBins) {
            var real = 0f
            var imag = 0f
            val angleStep = 2f * PI.toFloat() * bin / bufferSize
            for (i in 0 until n) {
                val windowed = pcm[i] * hannWindow[i]
                val angle = angleStep * i
                real += windowed * cos(angle)
                imag += windowed * sin(angle)
            }
            magnitudes[bin] = real * real + imag * imag
        }
        return magnitudes
    }

    private fun computeChromagram(): FloatArray {
        val spec = spectrum
        val chroma = FloatArray(12)
        val minBin = (55.0 / binWidth).toInt()      // A1 ~55 Hz
        val maxBin = minOf((2000.0 / binWidth).toInt(), bufferSize / 2)

        for (bin in minBin..maxBin) {
            val freq = bin * binWidth
            if (freq > 20f) {
                val midiNote = 12f * log2(freq / 440f) + 69f
                val pc = ((midiNote.roundToInt() % 12) + 12) % 12
                chroma[pc] += spec[bin]
            }
        }
        return chroma
    }

    companion object {
        private val hannWindow: FloatArray by lazy {
            FloatArray(AudioConfig.BUFFER_SIZE) { i ->
                0.5f * (1f - cos(2f * PI.toFloat() * i / (AudioConfig.BUFFER_SIZE - 1)))
            }
        }
    }
}
