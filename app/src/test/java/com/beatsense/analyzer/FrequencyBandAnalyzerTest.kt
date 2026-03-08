package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class FrequencyBandAnalyzerTest {

    private val analyzer = FrequencyBandAnalyzer()
    private val bufferSize = AudioConfig.BUFFER_SIZE
    private val sampleRate = AudioConfig.SAMPLE_RATE

    @Before
    fun setUp() {
        analyzer.reset()
    }

    @Test
    fun `returns Bands result type`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm))
        assertTrue(result is AnalyzerResult.Bands)
    }

    @Test
    fun `has five bands`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.Bands
        assertEquals(5, result.bands.size)
    }

    @Test
    fun `band names are correct`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.Bands
        assertEquals(listOf("Sub", "Bass", "Mids", "Pres", "Air"), result.bands.map { it.name })
    }

    @Test
    fun `label is SPECTRUM`() {
        val pcm = FloatArray(bufferSize) { 0f }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.Bands
        assertEquals("SPECTRUM", result.label)
    }

    @Test
    fun `silence produces zero bands`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f })) as AnalyzerResult.Bands
        result.bands.forEach { band ->
            assertEquals("Band ${band.name} should be 0 for silence", 0f, band.level, 0.001f)
        }
    }

    @Test
    fun `low frequency signal concentrates energy in bass bands`() {
        // 80 Hz sine — should light up the Bass band
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 80f * it / sampleRate) * 0.5f }
        repeat(5) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.Bands

        val bassLevel = result.bands.first { it.name == "Bass" }.level
        val airLevel = result.bands.first { it.name == "Air" }.level
        assertTrue("Bass should dominate over Air for 80 Hz signal, bass=$bassLevel air=$airLevel",
            bassLevel > airLevel)
    }

    @Test
    fun `mid frequency signal concentrates energy in mids band`() {
        // 1000 Hz sine — should light up Mids
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 1000f * it / sampleRate) * 0.5f }
        repeat(5) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.Bands

        val midsLevel = result.bands.first { it.name == "Mids" }.level
        assertTrue("Mids should be highest for 1000 Hz signal, mids=$midsLevel", midsLevel > 0.5f)
    }

    @Test
    fun `band levels are between 0 and 1`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.8f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        val result = analyzer.analyze(AudioFrame(pcm)) as AnalyzerResult.Bands

        result.bands.forEach { band ->
            assertTrue("Band ${band.name} level should be >= 0", band.level >= 0f)
            assertTrue("Band ${band.name} level should be <= 1", band.level <= 1f)
        }
    }

    @Test
    fun `reset clears state`() {
        val pcm = FloatArray(bufferSize) { sin(2f * PI.toFloat() * 440f * it / sampleRate) * 0.5f }
        repeat(10) { analyzer.analyze(AudioFrame(pcm)) }
        analyzer.reset()

        val result = analyzer.analyze(AudioFrame(FloatArray(bufferSize) { 0f })) as AnalyzerResult.Bands
        result.bands.forEach { band ->
            assertEquals("Band ${band.name} should be 0 after reset + silence", 0f, band.level, 0.001f)
        }
    }
}
