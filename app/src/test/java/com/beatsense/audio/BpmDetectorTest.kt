package com.beatsense.audio

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class BpmDetectorTest {

    private val bufferSize = AudioConfig.BUFFER_SIZE
    private val sampleRate = AudioConfig.SAMPLE_RATE
    private val buffersPerSecond = sampleRate.toFloat() / bufferSize

    @Before
    fun setUp() {
        BpmDetector.reset()
    }

    /**
     * Generate a sequence of buffers simulating periodic beats.
     * Each "beat" is a short burst of energy (2 buffers) followed by silence.
     */
    private fun feedBeatsAtBpm(bpm: Float, seconds: Float): Float {
        val buffersPerBeat = (60f / bpm * buffersPerSecond).toInt()
        val totalBuffers = (seconds * buffersPerSecond).toInt()
        var lastResult = 0f

        for (i in 0 until totalBuffers) {
            val posInBeat = i % buffersPerBeat
            val isBeat = posInBeat < 2 // 2 consecutive loud buffers per beat
            val buffer = if (isBeat) {
                // Energy burst — simulate a drum hit with decay
                val amp = if (posInBeat == 0) 0.6f else 0.3f
                FloatArray(bufferSize) { sin(2f * PI.toFloat() * 200f * it / sampleRate) * amp }
            } else {
                // Silence between beats
                FloatArray(bufferSize) { 0f }
            }
            lastResult = BpmDetector.detect(buffer)
        }

        return lastResult
    }

    @Test
    fun `no BPM reported before minimum accumulation`() {
        val silence = FloatArray(bufferSize) { 0f }
        // Feed less than 3 seconds of audio
        val buffersForTwoSeconds = (2f * buffersPerSecond).toInt()
        var result = 0f
        repeat(buffersForTwoSeconds) {
            result = BpmDetector.detect(silence)
        }
        assertEquals("Should not report BPM before 3 seconds", 0f, result, 0.001f)
    }

    @Test
    fun `silence produces no BPM`() {
        val silence = FloatArray(bufferSize) { 0f }
        var result = 0f
        repeat(100) {
            result = BpmDetector.detect(silence)
        }
        assertEquals("Silence should produce 0 BPM", 0f, result, 0.001f)
    }

    @Test
    fun `detects 120 BPM from periodic beats`() {
        val detectedBpm = feedBeatsAtBpm(120f, 10f)
        assertTrue(
            "Should detect ~120 BPM, got $detectedBpm",
            detectedBpm in 110f..130f
        )
    }

    @Test
    fun `detects 90 BPM from periodic beats`() {
        val detectedBpm = feedBeatsAtBpm(90f, 20f)
        assertTrue(
            "Should detect ~90 BPM, got $detectedBpm",
            detectedBpm in 75f..105f
        )
    }

    @Test
    fun `detects 150 BPM from periodic beats`() {
        val detectedBpm = feedBeatsAtBpm(150f, 15f)
        assertTrue(
            "Should detect ~150 BPM, got $detectedBpm",
            detectedBpm in 135f..165f
        )
    }

    @Test
    fun `confidence is zero for silence`() {
        val silence = FloatArray(bufferSize) { 0f }
        repeat(100) { BpmDetector.detect(silence) }
        assertEquals("Confidence should be 0 for silence", 0f, BpmDetector.getConfidence(), 0.001f)
    }

    @Test
    fun `reset clears state`() {
        feedBeatsAtBpm(120f, 6f)
        BpmDetector.reset()

        val silence = FloatArray(bufferSize) { 0f }
        repeat(50) { BpmDetector.detect(silence) }
        assertEquals("After reset and silence, BPM should be 0", 0f, BpmDetector.detect(silence), 0.001f)
    }

    @Test
    fun `BPM stays in valid range`() {
        // Feed chaotic random bursts — BPM should be 0 or within 55-185
        val random = java.util.Random(42)
        var result = 0f
        repeat(200) {
            val buffer = FloatArray(bufferSize) {
                if (random.nextFloat() > 0.95f) random.nextFloat() * 0.5f else 0f
            }
            result = BpmDetector.detect(buffer)
        }
        assertTrue(
            "BPM should be 0 or within 55-185, got $result",
            result == 0f || result in 55f..185f
        )
    }
}
