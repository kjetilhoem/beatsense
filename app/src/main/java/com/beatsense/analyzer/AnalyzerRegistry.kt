package com.beatsense.analyzer

/**
 * Orchestrates the analyzer pipeline — the "compositor".
 *
 * Builds an [AudioFrame] from raw PCM, fans out to all registered
 * analyzers, and collects results sorted by display priority.
 */
class AnalyzerRegistry {

    private val analyzers = mutableListOf<Analyzer>()

    fun register(analyzer: Analyzer) {
        analyzers.add(analyzer)
        analyzers.sortBy { it.displayPriority }
    }

    fun unregister(id: String) {
        analyzers.removeAll { it.id == id }
    }

    /**
     * Process a raw PCM buffer through all registered analyzers.
     * Returns results as an ordered list of (id, result) pairs.
     */
    fun process(buffer: FloatArray): List<Pair<String, AnalyzerResult>> {
        val frame = AudioFrame(buffer)
        return analyzers.map { analyzer ->
            analyzer.id to analyzer.analyze(frame)
        }
    }

    /** Reset all analyzers — call at session start */
    fun resetAll() {
        analyzers.forEach { it.reset() }
    }

    /** Currently registered analyzer count */
    val size: Int get() = analyzers.size
}
