package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class DynamicRangeAnalyzerTest {

    private val analyzer = DynamicRangeAnalyzer()
    private val bufferSize = AudioConfig.BUFFER_SIZE
    private val sampleRate = AudioConfig.SAMPLE_RATE

    @Before
    fun setUp() {
        analyzer.reset()
    }

    @Test
    fun `returns Pending during warmup`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.3f }
        // Feed less than warmup threshold (30 frames)
        repeat(5) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.Pending)
    }

    @Test
    fun `silence stays Pending`() {
        // Silence is gated out, so history never fills
        repeat(50) {
            analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }

    @Test
    fun `constant level produces narrow range`() {
        // Same amplitude throughout — dynamic range should be Flat or Narrow
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(40) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))

        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        assertTrue("Constant level should be Flat or Narrow, got: $value",
            value.contains("Flat") || value.contains("Narrow"))
    }

    @Test
    fun `varying levels produce wider range`() {
        // Alternate between loud and quiet signals
        val loud = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.8f }
        val quiet = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.02f }

        repeat(20) {
            analyzer.analyze(AudioFrame(loud))
            analyzer.analyze(AudioFrame(quiet))
        }
        val result = analyzer.analyze(AudioFrame(loud))

        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        // Should not be Flat — there's meaningful variation
        assertFalse("Varying levels should not be Flat, got: $value", value.contains("Flat"))
    }

    @Test
    fun `label is DYNAMIC RANGE`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm))
        when (result) {
            is AnalyzerResult.Pending -> assertEquals("DYNAMIC RANGE", result.label)
            is AnalyzerResult.HeroValue -> assertEquals("DYNAMIC RANGE", result.label)
            else -> fail("Unexpected result type")
        }
    }

    @Test
    fun `reset clears state`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(40) { analyzer.analyze(AudioFrame(pcm)) }
        analyzer.reset()
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue("After reset should be Pending", result is AnalyzerResult.Pending)
    }
}
