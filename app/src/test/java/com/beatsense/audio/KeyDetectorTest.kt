package com.beatsense.audio

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class KeyDetectorTest {

    private val sampleRate = AudioConfig.SAMPLE_RATE
    private val bufferSize = AudioConfig.BUFFER_SIZE

    @Before
    fun setUp() {
        KeyDetector.reset()
    }

    /**
     * Generate a buffer containing a pure sine wave at the given frequency.
     */
    private fun sineWave(frequency: Float, amplitude: Float = 0.5f): FloatArray {
        return FloatArray(bufferSize) { i ->
            amplitude * sin(2f * PI.toFloat() * frequency * i / sampleRate)
        }
    }

    /**
     * Generate a buffer containing multiple simultaneous sine waves.
     */
    private fun chord(frequencies: List<Float>, amplitude: Float = 0.3f): FloatArray {
        val buffer = FloatArray(bufferSize)
        for (freq in frequencies) {
            for (i in buffer.indices) {
                buffer[i] += amplitude * sin(2f * PI.toFloat() * freq * i / sampleRate)
            }
        }
        return buffer
    }

    @Test
    fun `silence returns no key`() {
        val silence = FloatArray(bufferSize) { 0f }
        // Feed enough frames to pass accumulation minimum
        var result: String? = null
        repeat(100) {
            result = KeyDetector.detect(silence)
        }
        assertTrue(
            "Silence should not produce a key, got: $result",
            result == null || result == "—"
        )
    }

    @Test
    fun `single note does not establish a key`() {
        // A4 = 440 Hz — a single pitch class cannot define a key
        val a4 = sineWave(440f)
        var result: String? = null
        repeat(100) {
            result = KeyDetector.detect(a4)
        }
        assertTrue(
            "A single note should not establish a key (psychoacoustic gating), got: $result",
            result == null || result == "—"
        )
    }

    @Test
    fun `two notes do not establish a key`() {
        // A4 + E5 — only 2 pitch classes, still insufficient
        val interval = chord(listOf(440f, 659.26f))
        var result: String? = null
        repeat(100) {
            result = KeyDetector.detect(interval)
        }
        assertTrue(
            "Two notes should not establish a key, got: $result",
            result == null || result == "—"
        )
    }

    @Test
    fun `C major chord produces a key detection`() {
        // C4 + E4 + G4 = C major triad — 3 pitch classes with tonal hierarchy
        // Use higher amplitude and add octave doubling for stronger spectral presence
        val cMajor = chord(listOf(261.63f, 329.63f, 392.00f, 523.25f), amplitude = 0.5f)
        var result: String? = null
        repeat(200) {
            result = KeyDetector.detect(cMajor)
        }
        assertNotNull("A major triad should eventually produce a key", result)
        assertNotEquals("A major triad should not return '—'", "—", result)
    }

    @Test
    fun `confidence is zero after reset`() {
        KeyDetector.reset()
        assertEquals(0f, KeyDetector.getConfidence(), 0.001f)
    }

    @Test
    fun `reset clears accumulated state`() {
        val cMajor = chord(listOf(261.63f, 329.63f, 392.00f))
        repeat(100) { KeyDetector.detect(cMajor) }

        KeyDetector.reset()

        val silence = FloatArray(bufferSize) { 0f }
        repeat(100) { KeyDetector.detect(silence) }

        val result = KeyDetector.detect(silence)
        assertTrue(
            "After reset and silence, should return no key, got: $result",
            result == null || result == "—"
        )
    }

    @Test
    fun `noise does not produce a key`() {
        val random = java.util.Random(42)
        var result: String? = null
        repeat(100) {
            val noise = FloatArray(bufferSize) { (random.nextFloat() * 2f - 1f) * 0.1f }
            result = KeyDetector.detect(noise)
        }
        assertTrue(
            "Random noise should not produce a confident key, got: $result",
            result == null || result == "—"
        )
    }
}
