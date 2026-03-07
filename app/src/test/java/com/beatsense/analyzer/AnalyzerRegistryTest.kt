package com.beatsense.analyzer

import org.junit.Test
import org.junit.Assert.*

class AnalyzerRegistryTest {

    /** Minimal test analyzer that counts calls */
    class CountingAnalyzer(
        override val id: String,
        override val displayPriority: Int = 0
    ) : Analyzer {
        override val name = "Counter"
        var analyzeCount = 0
        var resetCount = 0

        override fun analyze(frame: AudioFrame): AnalyzerResult {
            analyzeCount++
            return AnalyzerResult.Pending("test")
        }

        override fun reset() {
            resetCount++
        }
    }

    @Test
    fun `empty registry returns empty results`() {
        val registry = AnalyzerRegistry()
        val results = registry.process(FloatArray(4096) { 0f })
        assertTrue(results.isEmpty())
    }

    @Test
    fun `registered analyzer receives audio`() {
        val registry = AnalyzerRegistry()
        val counter = CountingAnalyzer("test")
        registry.register(counter)

        registry.process(FloatArray(4096) { 0f })
        assertEquals(1, counter.analyzeCount)

        registry.process(FloatArray(4096) { 0f })
        assertEquals(2, counter.analyzeCount)
    }

    @Test
    fun `results are ordered by display priority`() {
        val registry = AnalyzerRegistry()
        registry.register(CountingAnalyzer("c", displayPriority = 30))
        registry.register(CountingAnalyzer("a", displayPriority = 0))
        registry.register(CountingAnalyzer("b", displayPriority = 10))

        val results = registry.process(FloatArray(4096) { 0f })
        assertEquals(listOf("a", "b", "c"), results.map { it.first })
    }

    @Test
    fun `unregister removes analyzer`() {
        val registry = AnalyzerRegistry()
        val counter = CountingAnalyzer("test")
        registry.register(counter)
        assertEquals(1, registry.size)

        registry.unregister("test")
        assertEquals(0, registry.size)

        val results = registry.process(FloatArray(4096) { 0f })
        assertTrue(results.isEmpty())
        assertEquals(0, counter.analyzeCount) // never called — unregistered before process
    }

    @Test
    fun `resetAll resets all analyzers`() {
        val registry = AnalyzerRegistry()
        val a = CountingAnalyzer("a")
        val b = CountingAnalyzer("b")
        registry.register(a)
        registry.register(b)

        registry.resetAll()

        assertEquals(1, a.resetCount)
        assertEquals(1, b.resetCount)
    }

    @Test
    fun `multiple analyzers all receive same buffer`() {
        val registry = AnalyzerRegistry()
        val a = CountingAnalyzer("a")
        val b = CountingAnalyzer("b")
        val c = CountingAnalyzer("c")
        registry.register(a)
        registry.register(b)
        registry.register(c)

        registry.process(FloatArray(4096) { 0f })

        assertEquals(1, a.analyzeCount)
        assertEquals(1, b.analyzeCount)
        assertEquals(1, c.analyzeCount)
    }
}
