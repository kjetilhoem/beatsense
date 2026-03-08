package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import com.beatsense.audio.KeyDetector
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

/**
 * Tests for KeyAnalyzer with separated root note and mode.
 *
 * The key insight: root note and mode are independent observations.
 * Root can stay stable while mode flips between Major/Minor.
 */
class KeyAnalyzerTest {

    private lateinit var analyzer: KeyAnalyzer

    @Before
    fun setUp() {
        analyzer = KeyAnalyzer()
        analyzer.reset()
    }

    @Test
    fun `returns Pending before sufficient accumulation`() {
        val silence = FloatArray(AudioConfig.BUFFER_SIZE)
        val frame = AudioFrame(silence)
        val result = analyzer.analyze(frame)
        assertTrue("Should be Pending before accumulation", result is AnalyzerResult.Pending)
    }

    @Test
    fun `returns ValueGroup with root and mode when key is detected`() {
        // Build a C major chord: C4 (261.63), E4 (329.63), G4 (392.00), C5 (523.25)
        val frequencies = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f)
        val amplitude = 0.5f

        // Feed enough frames to accumulate evidence
        for (i in 0 until 200) {
            val buffer = generateChord(frequencies, amplitude, i)
            val frame = AudioFrame(buffer)
            analyzer.analyze(frame)
        }

        // Get final result
        val buffer = generateChord(frequencies, amplitude, 200)
        val result = analyzer.analyze(AudioFrame(buffer))

        // Should be a ValueGroup with root and mode as separate values
        assertTrue("Should be ValueGroup, got $result", result is AnalyzerResult.ValueGroup)
        val group = result as AnalyzerResult.ValueGroup
        assertEquals("KEY", group.label)
        assertEquals(2, group.values.size)

        val rootValue = group.values.find { it.label == "Root" }
        val modeValue = group.values.find { it.label == "Mode" }
        assertNotNull("Should have Root value", rootValue)
        assertNotNull("Should have Mode value", modeValue)

        // Root should be a note name (C, D, E, etc.)
        assertTrue("Root should be a note name, got '${rootValue!!.value}'",
            rootValue.value.matches(Regex("[A-G]#?")))

        // Mode should be Major or Minor
        assertTrue("Mode should be Major or Minor, got '${modeValue!!.value}'",
            modeValue!!.value == "Major" || modeValue.value == "Minor")
    }

    @Test
    fun `root and mode are reported independently`() {
        // The key result should separate root from mode
        // so the UI can display them in different positions/sizes
        val frequencies = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f)

        for (i in 0 until 200) {
            val buffer = generateChord(frequencies, 0.5f, i)
            analyzer.analyze(AudioFrame(buffer))
        }

        val result = analyzer.analyze(AudioFrame(generateChord(frequencies, 0.5f, 200)))
        if (result is AnalyzerResult.ValueGroup) {
            val root = result.values.find { it.label == "Root" }
            val mode = result.values.find { it.label == "Mode" }
            // Root should not contain "Major" or "Minor"
            assertFalse("Root should not contain mode",
                root!!.value.contains("Major") || root.value.contains("Minor"))
            // Mode should not contain note names
            assertFalse("Mode should not contain root note",
                mode!!.value.matches(Regex(".*[A-G]#?.*")))
        }
    }

    @Test
    fun `silence returns Pending with dash`() {
        val silence = FloatArray(AudioConfig.BUFFER_SIZE)
        // Feed many silent frames
        for (i in 0 until 100) {
            analyzer.analyze(AudioFrame(silence))
        }
        val result = analyzer.analyze(AudioFrame(silence))
        assertTrue("Silence should return Pending", result is AnalyzerResult.Pending)
    }

    @Test
    fun `reset clears state`() {
        val frequencies = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f)
        for (i in 0 until 100) {
            analyzer.analyze(AudioFrame(generateChord(frequencies, 0.5f, i)))
        }
        analyzer.reset()
        val result = analyzer.analyze(AudioFrame(FloatArray(AudioConfig.BUFFER_SIZE)))
        assertTrue("After reset should be Pending", result is AnalyzerResult.Pending)
    }

    @Test
    fun `id is key`() {
        assertEquals("key", analyzer.id)
    }

    @Test
    fun `display priority is 10`() {
        assertEquals(10, analyzer.displayPriority)
    }

    // --- Helpers ---

    private fun generateChord(frequencies: FloatArray, amplitude: Float, frameIndex: Int): FloatArray {
        val buffer = FloatArray(AudioConfig.BUFFER_SIZE)
        val samplesPerFrame = AudioConfig.BUFFER_SIZE
        val startSample = frameIndex * samplesPerFrame

        for (i in buffer.indices) {
            var sample = 0f
            for (freq in frequencies) {
                sample += amplitude * sin(
                    2.0 * PI * freq * (startSample + i) / AudioConfig.SAMPLE_RATE
                ).toFloat()
            }
            buffer[i] = sample / frequencies.size
        }
        return buffer
    }
}
