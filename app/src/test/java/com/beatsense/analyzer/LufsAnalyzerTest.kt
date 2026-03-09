package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class LufsAnalyzerTest {

    private val analyzer = LufsAnalyzer()
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
    fun `returns ValueGroup after warmup with signal`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.ValueGroup)
        val group = result as AnalyzerResult.ValueGroup
        assertEquals(3, group.values.size)
        assertEquals("Momentary", group.values[0].label)
        assertEquals("Short-term", group.values[1].label)
        assertEquals("Integrated", group.values[2].label)
    }

    @Test
    fun `silence returns dash values`() {
        repeat(10) { analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f })) }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.ValueGroup)
        val group = result as AnalyzerResult.ValueGroup
        assertTrue(group.values.all { it.value == "—" })
    }

    @Test
    fun `louder signal produces higher LUFS`() {
        val quiet = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.1f }
        val loud = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.8f }

        repeat(10) { analyzer.analyze(AudioFrame(quiet)) }
        val quietResult = analyzer.analyze(AudioFrame(quiet)) as AnalyzerResult.ValueGroup
        val quietMomentary = quietResult.values[0].value

        analyzer.reset()
        repeat(10) { analyzer.analyze(AudioFrame(loud)) }
        val loudResult = analyzer.analyze(AudioFrame(loud)) as AnalyzerResult.ValueGroup
        val loudMomentary = loudResult.values[0].value

        // Both should be numeric (not dash)
        assertNotEquals("—", quietMomentary)
        assertNotEquals("—", loudMomentary)
        // Loud should be higher (less negative) than quiet
        assertTrue(loudMomentary.toFloat() > quietMomentary.toFloat())
    }

    @Test
    fun `label is LOUDNESS`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.ValueGroup)
        assertTrue((result as AnalyzerResult.ValueGroup).label.contains("LOUDNESS"))
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
