package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class TransientDensityAnalyzerTest {

    private val analyzer = TransientDensityAnalyzer()
    private val bufferSize = AudioConfig.BUFFER_SIZE
    private val sampleRate = AudioConfig.SAMPLE_RATE

    @Before
    fun setUp() {
        analyzer.reset()
    }

    @Test
    fun `returns Pending while accumulating`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }

    @Test
    fun `silence produces sparse transients`() {
        // Fill the window with silence
        repeat(50) {
            analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))

        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        assertTrue("Silence should be Sparse, got: $value", value.contains("Sparse"))
    }

    @Test
    fun `alternating loud and quiet produces transients`() {
        // Alternate between silence and loud bursts to simulate onsets
        repeat(50) { i ->
            val pcm = if (i % 4 == 0) {
                FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
            } else {
                FloatArray(bufferSize) { 0.001f }  // near silence
            }
            analyzer.analyze(AudioFrame(pcm))
        }

        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0.001f }))
        assertTrue("Should return HeroValue after warmup", result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        // Should detect some transients from the alternating pattern
        assertFalse("Should not be silent/empty", value == "—")
    }

    @Test
    fun `label is TRANSIENTS`() {
        repeat(50) {
            analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        when (result) {
            is AnalyzerResult.HeroValue -> assertEquals("TRANSIENTS", result.label)
            is AnalyzerResult.Pending -> assertEquals("TRANSIENTS", result.label)
            else -> fail("Unexpected result type")
        }
    }

    @Test
    fun `reset clears state`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(50) { analyzer.analyze(AudioFrame(pcm)) }
        analyzer.reset()
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }
}
