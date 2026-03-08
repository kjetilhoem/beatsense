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
    fun `silence produces dash values after warmup`() {
        repeat(10) {
            analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        }
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))

        assertTrue(result is AnalyzerResult.ValueGroup)
        val group = result as AnalyzerResult.ValueGroup
        assertEquals(3, group.values.size)
        // All values should be "—" for silence
        group.values.forEach { lv ->
            assertEquals("Silence should show dash for ${lv.label}", "—", lv.value)
        }
    }

    @Test
    fun `has three measurements M S I`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.3f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm))

        assertTrue(result is AnalyzerResult.ValueGroup)
        val group = result as AnalyzerResult.ValueGroup
        assertEquals(listOf("M", "S", "I"), group.values.map { it.label })
    }

    @Test
    fun `all values have LUFS unit`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.3f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.ValueGroup

        result.values.forEach { lv ->
            assertEquals("LUFS", lv.unit)
        }
    }

    @Test
    fun `label is LOUDNESS`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.3f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.ValueGroup
        assertEquals("LOUDNESS", result.label)
    }

    @Test
    fun `loud signal produces negative LUFS values`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.ValueGroup

        // Momentary should be a negative number (not dash)
        val momentary = result.values.first { it.label == "M" }
        assertNotEquals("Loud signal should produce a value, not dash", "—", momentary.value)
        val lufsValue = momentary.value.toFloatOrNull()
        assertNotNull("Should be a parseable number", lufsValue)
        assertTrue("LUFS should be negative, got: $lufsValue", lufsValue!! < 0f)
    }

    @Test
    fun `louder signal has higher LUFS than quieter`() {
        val quiet = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.1f }
        val loud = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }

        // Measure quiet
        val quietAnalyzer = LufsAnalyzer()
        repeat(10) { quietAnalyzer.analyze(AudioFrame(quiet)) }
        val quietResult = quietAnalyzer.analyze(AudioFrame(quiet)) as AnalyzerResult.ValueGroup
        val quietLufs = quietResult.values.first { it.label == "M" }.value.toFloat()

        // Measure loud
        val loudAnalyzer = LufsAnalyzer()
        repeat(10) { loudAnalyzer.analyze(AudioFrame(loud)) }
        val loudResult = loudAnalyzer.analyze(AudioFrame(loud)) as AnalyzerResult.ValueGroup
        val loudLufs = loudResult.values.first { it.label == "M" }.value.toFloat()

        assertTrue("Loud ($loudLufs) should be higher than quiet ($quietLufs)", loudLufs > quietLufs)
    }

    @Test
    fun `reset clears state`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.3f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        analyzer.reset()
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f }))
        assertTrue(result is AnalyzerResult.Pending)
    }
}
