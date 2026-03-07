package com.beatsense.analyzer

import com.beatsense.audio.AudioConfig
import com.beatsense.audio.BpmDetector
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class BpmAnalyzerTest {

    private val bufferSize = AudioConfig.BUFFER_SIZE
    private val sampleRate = AudioConfig.SAMPLE_RATE
    private val buffersPerSecond = sampleRate.toFloat() / bufferSize
    private val analyzer = BpmAnalyzer()

    @Before
    fun setUp() {
        analyzer.reset()
    }

    @Test
    fun `returns Pending before detection`() {
        val frame = AudioFrame(FloatArray(bufferSize) { 0f })
        val result = analyzer.analyze(frame)
        assertTrue("Should return Pending for silence", result is AnalyzerResult.Pending)
    }

    @Test
    fun `returns HeroValue with accent after detecting BPM`() {
        val buffersPerBeat = (60f / 120f * buffersPerSecond).toInt()
        val totalBuffers = (12f * buffersPerSecond).toInt()

        var lastResult: AnalyzerResult? = null
        for (i in 0 until totalBuffers) {
            val posInBeat = i % buffersPerBeat
            val isBeat = posInBeat < 2
            val pcm = if (isBeat) {
                val amp = if (posInBeat == 0) 0.6f else 0.3f
                FloatArray(bufferSize) { sin(2f * PI.toFloat() * 200f * it / sampleRate) * amp }
            } else {
                FloatArray(bufferSize) { 0f }
            }
            lastResult = analyzer.analyze(AudioFrame(pcm))
        }

        assertTrue("Should return HeroValue after detection, got $lastResult", lastResult is AnalyzerResult.HeroValue)
        val hero = lastResult as AnalyzerResult.HeroValue
        assertEquals("BPM", hero.label)
        assertTrue("Should have accent color", hero.accentColor)
        assertTrue("Confidence should be > 0", hero.confidence > 0f)
    }
}
