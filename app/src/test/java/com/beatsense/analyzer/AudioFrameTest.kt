package com.beatsense.analyzer

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class AudioFrameTest {

    @Test
    fun `rms of silence is zero`() {
        val frame = AudioFrame(FloatArray(4096) { 0f })
        assertEquals(0f, frame.rms, 0.001f)
    }

    @Test
    fun `rms of known signal is correct`() {
        // Constant signal of 0.5 has RMS = 0.5
        val frame = AudioFrame(FloatArray(4096) { 0.5f })
        assertEquals(0.5f, frame.rms, 0.001f)
    }

    @Test
    fun `spectrum has correct number of bins`() {
        val frame = AudioFrame(FloatArray(4096) { 0f })
        assertEquals(4096 / 2 + 1, frame.spectrum.size)
    }

    @Test
    fun `spectrum detects a 440 Hz sine wave`() {
        val sampleRate = 44100
        val bufferSize = 4096
        val freq = 440f
        val pcm = FloatArray(bufferSize) { i ->
            sin(2f * PI.toFloat() * freq * i / sampleRate) * 0.5f
        }
        val frame = AudioFrame(pcm, sampleRate, bufferSize)

        // Expected bin for 440 Hz
        val expectedBin = (freq / frame.binWidth).roundToInt()
        val peakBin = frame.spectrum.indices.maxByOrNull { frame.spectrum[it] } ?: -1

        assertTrue(
            "Peak should be near bin $expectedBin (440 Hz), was at bin $peakBin",
            abs(peakBin - expectedBin) <= 1
        )
    }

    @Test
    fun `chromagram has 12 bins`() {
        val frame = AudioFrame(FloatArray(4096) { 0f })
        assertEquals(12, frame.chromagram.size)
    }

    @Test
    fun `chromagram concentrates energy for A440 in pitch class A`() {
        val sampleRate = 44100
        val bufferSize = 4096
        val pcm = FloatArray(bufferSize) { i ->
            sin(2f * PI.toFloat() * 440f * i / sampleRate) * 0.5f
        }
        val frame = AudioFrame(pcm, sampleRate, bufferSize)

        // A is pitch class 9 (C=0, C#=1, ..., A=9)
        val peakPc = frame.chromagram.indices.maxByOrNull { frame.chromagram[it] } ?: -1
        assertEquals("A440 should map to pitch class A (9)", 9, peakPc)
    }

    @Test
    fun `lazy computation does not recompute`() {
        val frame = AudioFrame(FloatArray(4096) { 0.1f })
        val s1 = frame.spectrum
        val s2 = frame.spectrum
        assertSame("Spectrum should be the same object on second access", s1, s2)
    }
}
