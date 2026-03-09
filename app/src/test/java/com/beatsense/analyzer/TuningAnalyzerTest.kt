package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class TuningAnalyzerTest {

    private val analyzer = TuningAnalyzer()
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
        repeat(15) { analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f })) }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.HeroValue)
        assertEquals("—", (result as AnalyzerResult.HeroValue).value)
    }

    @Test
    fun `standard 440 Hz signal detects standard tuning`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(15) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        // Should detect near 440 Hz
        assertTrue("440 Hz should be Standard, got: $value",
            value.contains("Standard") || value.contains("440"))
    }

    @Test
    fun `A220 signal still references A4`() {
        // 220 Hz is A3 — should still compute A4 reference ~440
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 220f * it / sampleRate) * 0.5f }
        repeat(15) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.HeroValue)
        val value = (result as AnalyzerResult.HeroValue).value
        assertTrue("A220 should reference near A4=440, got: $value",
            value.contains("A=") && !value.contains("—"))
    }

    @Test
    fun `label is TUNING`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm))
        when (result) {
            is AnalyzerResult.HeroValue -> assertEquals("TUNING", result.label)
            is AnalyzerResult.Pending -> assertEquals("TUNING", result.label)
            else -> fail("Unexpected result type")
        }
    }

    @Test
    fun `reset clears state`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(15) { analyzer.analyze(AudioFrame(pcm)) }
        analyzer.reset()
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }
}
