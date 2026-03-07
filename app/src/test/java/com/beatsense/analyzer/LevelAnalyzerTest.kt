package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LevelAnalyzerTest {

    private val analyzer = LevelAnalyzer()

    @Before
    fun setUp() {
        analyzer.reset()
    }

    @Test
    fun `silence produces zero meter`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(AudioConfig.BUFFER_SIZE) { 0f }))
        assertTrue(result is AnalyzerResult.Meter)
        assertEquals(0f, (result as AnalyzerResult.Meter).level, 0.001f)
    }

    @Test
    fun `loud signal produces high meter`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(AudioConfig.BUFFER_SIZE) { 0.5f }))
        assertTrue(result is AnalyzerResult.Meter)
        assertTrue((result as AnalyzerResult.Meter).level > 0.5f)
    }

    @Test
    fun `meter label is LEVEL`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(AudioConfig.BUFFER_SIZE) { 0f }))
        assertEquals("LEVEL", (result as AnalyzerResult.Meter).label)
    }
}
