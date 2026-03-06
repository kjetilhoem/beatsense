package com.beatsense.audio

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AudioAnalyzerTest {

    @Before
    fun setUp() {
        AudioAnalyzer.reset()
    }

    @Test
    fun `silence produces zero level`() {
        val silence = FloatArray(1024) { 0f }
        val level = AudioAnalyzer.computeLevel(silence)
        assertEquals(0f, level, 0.001f)
    }

    @Test
    fun `loud signal produces high level`() {
        val loud = FloatArray(1024) { 0.5f }
        val level = AudioAnalyzer.computeLevel(loud)
        assertTrue("Level should be > 0.5 for a loud signal, was $level", level > 0.5f)
    }

    @Test
    fun `level is clamped to 0-1 range`() {
        val clipping = FloatArray(1024) { 1.0f }
        val level = AudioAnalyzer.computeLevel(clipping)
        assertTrue("Level should be <= 1.0, was $level", level <= 1.0f)
        assertTrue("Level should be >= 0.0, was $level", level >= 0.0f)
    }

    @Test
    fun `fast attack - level rises quickly on transient`() {
        val silence = FloatArray(1024) { 0f }
        AudioAnalyzer.computeLevel(silence)

        val transient = FloatArray(1024) { 0.3f }
        val afterTransient = AudioAnalyzer.computeLevel(transient)
        assertTrue("Level should rise quickly on attack, was $afterTransient", afterTransient > 0.5f)
    }

    @Test
    fun `slow release - level decays gradually after transient`() {
        val loud = FloatArray(1024) { 0.3f }
        AudioAnalyzer.computeLevel(loud)
        val peakLevel = AudioAnalyzer.computeLevel(loud)

        val silence = FloatArray(1024) { 0f }
        val afterOneSilence = AudioAnalyzer.computeLevel(silence)

        assertTrue(
            "Level should decay slowly (release). Peak=$peakLevel, after silence=$afterOneSilence",
            afterOneSilence > peakLevel * 0.8f
        )
    }

    @Test
    fun `reset clears state`() {
        val loud = FloatArray(1024) { 0.5f }
        AudioAnalyzer.computeLevel(loud)

        AudioAnalyzer.reset()

        val silence = FloatArray(1024) { 0f }
        val level = AudioAnalyzer.computeLevel(silence)
        assertEquals("After reset, silence should produce zero", 0f, level, 0.001f)
    }
}
