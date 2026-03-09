package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class CrestFactorAnalyzerTest {

    private val analyzer = CrestFactorAnalyzer()
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
    fun `sine wave has known crest factor`() {
        // A pure sine has a crest factor of ~3 dB
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        // Should be "Compressed" (< 6 dB for pure sine ~3 dB)
        assertTrue("Sine wave should be Compressed, got: $value", value.contains("Compressed"))
    }

    @Test
    fun `label is DYNAMICS`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm))
        when (result) {
            is AnalyzerResult.HeroValue -> assertEquals("DYNAMICS", result.label)
            is AnalyzerResult.Pending -> assertEquals("DYNAMICS", result.label)
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
