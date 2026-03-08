package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

/**
 * Tests for SpectralCentroidAnalyzer.
 *
 * Spectral centroid = weighted mean frequency of the spectrum.
 * Low centroid = dark/warm sound. High centroid = bright/airy sound.
 */
class SpectralCentroidAnalyzerTest {

    private lateinit var analyzer: SpectralCentroidAnalyzer

    @Before
    fun setUp() {
        analyzer = SpectralCentroidAnalyzer()
    }

    @Test
    fun `silence returns Pending`() {
        val silence = FloatArray(AudioConfig.BUFFER_SIZE)
        val result = analyzer.analyze(AudioFrame(silence))
        assertTrue("Silence should return Pending", result is AnalyzerResult.Pending)
    }

    @Test
    fun `low frequency tone has low centroid`() {
        // 100 Hz sine — should produce centroid near 100 Hz
        val buffer = generateSine(100f)
        val result = analyzer.analyze(AudioFrame(buffer))
        assertTrue("Should be HeroValue, got $result", result is AnalyzerResult.HeroValue)
        val hero = result as AnalyzerResult.HeroValue
        val hz = hero.value.replace(" Hz", "").toFloat()
        assertTrue("100 Hz tone should have centroid < 300 Hz, got $hz", hz < 300f)
    }

    @Test
    fun `high frequency tone has high centroid`() {
        // 4000 Hz sine — should produce centroid near 4000 Hz
        val buffer = generateSine(4000f)
        val result = analyzer.analyze(AudioFrame(buffer))
        assertTrue("Should be HeroValue", result is AnalyzerResult.HeroValue)
        val hero = result as AnalyzerResult.HeroValue
        val hz = hero.value.replace(" Hz", "").toFloat()
        assertTrue("4000 Hz tone should have centroid > 2000 Hz, got $hz", hz > 2000f)
    }

    @Test
    fun `centroid increases with higher frequency content`() {
        val low = analyzer.analyze(AudioFrame(generateSine(200f)))
        analyzer.reset()
        val high = analyzer.analyze(AudioFrame(generateSine(3000f)))

        assertTrue(low is AnalyzerResult.HeroValue)
        assertTrue(high is AnalyzerResult.HeroValue)

        val lowHz = (low as AnalyzerResult.HeroValue).value.replace(" Hz", "").toFloat()
        val highHz = (high as AnalyzerResult.HeroValue).value.replace(" Hz", "").toFloat()
        assertTrue("Higher frequency should have higher centroid: $lowHz vs $highHz", highHz > lowHz)
    }

    @Test
    fun `id is spectral-centroid`() {
        assertEquals("spectral-centroid", analyzer.id)
    }

    @Test
    fun `display priority is 35`() {
        assertEquals(35, analyzer.displayPriority)
    }

    @Test
    fun `reset does not crash`() {
        analyzer.analyze(AudioFrame(generateSine(440f)))
        analyzer.reset()
        // Stateless analyzer — reset is a no-op but must not throw
    }

    // --- Helpers ---

    private fun generateSine(freq: Float, amplitude: Float = 0.5f): FloatArray {
        return FloatArray(AudioConfig.BUFFER_SIZE) { i ->
            amplitude * sin(2.0 * PI * freq * i / AudioConfig.SAMPLE_RATE).toFloat()
        }
    }
}
