package com.beatsense.analyzer

/**
 * Contract for an audio analyzer — the "surface shader" interface.
 *
 * Each analyzer receives a pre-computed [AudioFrame] and returns
 * a typed [AnalyzerResult]. Analyzers are isolated — they cannot
 * reference each other and must manage their own internal state.
 *
 * Must be fast: called on the audio thread at ~10 Hz.
 */
interface Analyzer {

    /** Unique identifier, used for registration and state lookup */
    val id: String

    /** Human-readable name shown in UI */
    val name: String

    /** Display ordering. Lower = higher on screen. BPM=0, Key=10, Level=20, etc. */
    val displayPriority: Int

    /** Process one audio frame. Must be non-blocking. */
    fun analyze(frame: AudioFrame): AnalyzerResult

    /** Reset all accumulated state. Called at session start. */
    fun reset()
}
