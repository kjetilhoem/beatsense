package com.beatsense

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.beatsense.analyzer.AnalyzerRegistry
import com.beatsense.analyzer.AnalyzerResult
import com.beatsense.analyzer.BpmAnalyzer
import com.beatsense.analyzer.KeyAnalyzer
import com.beatsense.analyzer.FrequencyBandAnalyzer
import com.beatsense.analyzer.LevelAnalyzer
import com.beatsense.audio.AudioCaptureService
import com.beatsense.ui.BeatSenseScreen
import com.beatsense.ui.CaptureMode

class MainActivity : ComponentActivity() {

    private val bpmState = mutableFloatStateOf(0f)
    private val keyState = mutableStateOf("—")
    private val isCapturing = mutableStateOf(false)
    private val audioLevel = mutableFloatStateOf(0f)
    private val bpmConfidence = mutableFloatStateOf(0f)
    private val keyConfidence = mutableFloatStateOf(0f)
    private val captureMode = mutableStateOf(CaptureMode.APP_AUDIO)
    private val frequencyBands = mutableStateListOf<AnalyzerResult.Bands.Band>()

    private val registry = AnalyzerRegistry().apply {
        register(BpmAnalyzer())
        register(KeyAnalyzer())
        register(LevelAnalyzer())
        register(FrequencyBandAnalyzer())
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startCaptureService(
                mode = AudioCaptureService.MODE_APP_AUDIO,
                resultCode = result.resultCode,
                data = result.data
            )
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCaptureService(mode = AudioCaptureService.MODE_MICROPHONE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BeatSenseScreen(
                bpm = bpmState.floatValue,
                musicalKey = keyState.value,
                isCapturing = isCapturing.value,
                audioLevel = audioLevel.floatValue,
                bpmConfidence = bpmConfidence.floatValue,
                keyConfidence = keyConfidence.floatValue,
                frequencyBands = frequencyBands,
                captureMode = captureMode.value,
                onModeChanged = { mode -> captureMode.value = mode },
                onStartCapture = { startCapture() },
                onStopCapture = { stopCaptureService() }
            )
        }
    }

    private fun startCapture() {
        when (captureMode.value) {
            CaptureMode.APP_AUDIO -> {
                val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
            }
            CaptureMode.MICROPHONE -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startCaptureService(mode = AudioCaptureService.MODE_MICROPHONE)
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    private fun startCaptureService(mode: String, resultCode: Int? = null, data: Intent? = null) {
        registry.resetAll()

        AudioCaptureService.onAudioData = { buffer ->
            val results = registry.process(buffer)
            for ((id, result) in results) {
                when (id) {
                    "bpm" -> when (result) {
                        is AnalyzerResult.HeroValue -> {
                            bpmState.floatValue = result.value.toFloatOrNull() ?: 0f
                            bpmConfidence.floatValue = result.confidence
                        }
                        is AnalyzerResult.Pending -> {} // keep last value
                        else -> {}
                    }
                    "key" -> when (result) {
                        is AnalyzerResult.HeroValue -> {
                            keyState.value = result.value
                            keyConfidence.floatValue = result.confidence
                        }
                        is AnalyzerResult.Pending -> {
                            keyState.value = "—"
                            keyConfidence.floatValue = 0f
                        }
                        else -> {}
                    }
                    "level" -> when (result) {
                        is AnalyzerResult.Meter -> {
                            audioLevel.floatValue = result.level
                        }
                        else -> {}
                    }
                    "bands" -> when (result) {
                        is AnalyzerResult.Bands -> {
                            frequencyBands.clear()
                            frequencyBands.addAll(result.bands)
                        }
                        else -> {}
                    }
                }
            }
        }

        val serviceIntent = Intent(this, AudioCaptureService::class.java).apply {
            putExtra("mode", mode)
            if (resultCode != null) putExtra("resultCode", resultCode)
            if (data != null) putExtra("data", data)
        }
        startForegroundService(serviceIntent)
        isCapturing.value = true
    }

    private fun stopCaptureService() {
        stopService(Intent(this, AudioCaptureService::class.java))
        isCapturing.value = false
        AudioCaptureService.onAudioData = null
    }
}
