package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI
import kotlin.math.sin

class FrequencyBandAnalyzerTest {

    private val analyzer = FrequencyBandAnalyzer()

    @Before
    fun setUp() {
        analyzer.reset()
    }

    private fun sineWave(frequencyHz: Float): FloatArray {
        return FloatArray(AudioConfig.BUFFER_SIZE) { i ->
            sin(2.0 * PI * frequencyHz * i / AudioConfig.SAMPLE_RATE).toFloat()
        }
    }

    @Test
    fun `silence produces zero energy in all bands`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(AudioConfig.BUFFER_SIZE) { 0f }))
        assertTrue(result is AnalyzerResult.Bands)
        val bands = (result as AnalyzerResult.Bands).bands
        for (band in bands) {
            assertEquals("${band.name} should be zero", 0f, band.level, 0.001f)
        }
    }

    @Test
    fun `100 Hz sine has energy primarily in bass band`() {
        val result = analyzer.analyze(AudioFrame(sineWave(100f)))
        val bands = (result as AnalyzerResult.Bands).bands
        val bass = bands.first { it.name == "Bass" }
        val others = bands.filter { it.name != "Bass" }
        assertTrue("Bass should have significant energy", bass.level > 0.5f)
        for (other in others) {
            assertTrue("${other.name} should be less than Bass", other.level < bass.level)
        }
    }

    @Test
    fun `1 kHz sine has energy primarily in mids band`() {
        val result = analyzer.analyze(AudioFrame(sineWave(1000f)))
        val bands = (result as AnalyzerResult.Bands).bands
        val mids = bands.first { it.name == "Mids" }
        val others = bands.filter { it.name != "Mids" }
        assertTrue("Mids should have significant energy", mids.level > 0.5f)
        for (other in others) {
            assertTrue("${other.name} should be less than Mids", other.level < mids.level)
        }
    }

    @Test
    fun `6 kHz sine has energy in presence band`() {
        val result = analyzer.analyze(AudioFrame(sineWave(6000f)))
        val bands = (result as AnalyzerResult.Bands).bands
        val presence = bands.first { it.name == "Presence" }
        assertTrue("Presence should have significant energy", presence.level > 0.5f)
    }

    @Test
    fun `reset does not crash`() {
        analyzer.reset()
    }

    @Test
    fun `all 5 band names are present`() {
        val result = analyzer.analyze(AudioFrame(FloatArray(AudioConfig.BUFFER_SIZE) { 0f }))
        val bandNames = (result as AnalyzerResult.Bands).bands.map { it.name }
        assertEquals(5, bandNames.size)
        assertTrue(bandNames.contains("Sub"))
        assertTrue(bandNames.contains("Bass"))
        assertTrue(bandNames.contains("Mids"))
        assertTrue(bandNames.contains("Presence"))
        assertTrue(bandNames.contains("Air"))
    }
}
