package com.beatsense.analyzer

/**
 * Typed output from an analyzer — the "surface shader output struct".
 *
 * The UI renders each subtype with a matching card. Adding a new analyzer
 * that returns an existing result type gets a card for free.
 */
sealed class AnalyzerResult {

    /** A single prominent value with optional confidence (BPM, key, transient density) */
    data class HeroValue(
        val value: String,
        val label: String,
        val confidence: Float = 0f,
        val accentColor: Boolean = false
    ) : AnalyzerResult()

    /** A 0-1 level (audio level, crest factor) */
    data class Meter(
        val level: Float,
        val label: String,
        val segments: Int = 32,
        val colorZones: Boolean = true
    ) : AnalyzerResult()

    /** Multiple labeled bands (frequency bands, stereo L/R) */
    data class Bands(
        val label: String,
        val bands: List<Band>
    ) : AnalyzerResult() {
        data class Band(val name: String, val level: Float)
    }

    /** Multiple related values in a group (LUFS-I, LUFS-S, LUFS-M) */
    data class ValueGroup(
        val label: String,
        val values: List<LabeledValue>
    ) : AnalyzerResult() {
        data class LabeledValue(val label: String, val value: String, val unit: String = "")
    }

    /** No result yet (insufficient data, warming up) */
    data class Pending(val label: String, val reason: String = "") : AnalyzerResult()
}
