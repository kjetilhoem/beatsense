package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class SpectralRolloffAnalyzerTest {

    private val analyzer = SpectralRolloffAnalyzer()
    private val bufferSize = AudioConfig.BUFFER_SIZE
    private val sampleRate = AudioConfig.SAMPLE_RATE

    @Before
    fun setUp() {
        analyzer.reset()
    }

    @Test
    fun `returns Pending initially`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }

    @Test
    fun `silence returns dash after warmup`() {
        repeat(10) { analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f })) }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.HeroValue)
        assertEquals("—", (result as AnalyzerResult.HeroValue).value)
    }

    @Test
    fun `low frequency signal has low rolloff`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 100f * it / sampleRate) * 0.5f }
        repeat(5) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        assertTrue("Low freq should be Dark or Warm, got: $value", value.contains("Dark") || value.contains("Warm"))
    }

    @Test
    fun `high frequency signal has high rolloff`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 8000f * it / sampleRate) * 0.5f }
        repeat(5) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        assertTrue("High freq should be Bright or Airy, got: $value", value.contains("Bright") || value.contains("Airy"))
    }

    @Test
    fun `label is ROLLOFF`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm))
        when (result) {
            is AnalyzerResult.HeroValue -> assertEquals("ROLLOFF", result.label)
            is AnalyzerResult.Pending -> assertEquals("ROLLOFF", result.label)
            else -> fail("Unexpected result type")
        }
    }

    @Test
    fun `reset clears state`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        analyzer.reset()
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }
}
