package com.beatsense.analyzer

import com.beatsense.audio.KeyDetector

/**
 * Key analyzer — reports root note and mode (Major/Minor) independently.
 *
 * Uses raw PCM (KeyDetector has its own windowing and DFT internally).
 * Returns ValueGroup with separate Root and Mode values, allowing
 * mode to update without root changing in the UI.
 */
class KeyAnalyzer : Analyzer {

    override val id = "key"
    override val name = "Key"
    override val displayPriority = 10

    override fun analyze(frame: AudioFrame): AnalyzerResult {
        KeyDetector.detect(frame.pcm)
        val keyResult = KeyDetector.getKeyResult()

        return when {
            keyResult.root == null || keyResult.mode == null ->
                AnalyzerResult.Pending("KEY", "Accumulating...")

            else -> AnalyzerResult.ValueGroup(
                label = "KEY",
                values = listOf(
                    AnalyzerResult.ValueGroup.LabeledValue(
                        label = "Root",
                        value = keyResult.root
                    ),
                    AnalyzerResult.ValueGroup.LabeledValue(
                        label = "Mode",
                        value = keyResult.mode
                    )
                )
            )
        }
    }

    override fun reset() {
        KeyDetector.reset()
    }
}
