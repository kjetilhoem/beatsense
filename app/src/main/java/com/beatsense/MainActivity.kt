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
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.beatsense.audio.AudioAnalyzer
import com.beatsense.audio.AudioCaptureService
import com.beatsense.audio.BpmDetector
import com.beatsense.audio.KeyDetector
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
        // Reset analyzers for fresh session
        BpmDetector.reset()
        KeyDetector.reset()
        AudioAnalyzer.reset()

        AudioCaptureService.onAudioData = { buffer ->
            audioLevel.floatValue = AudioAnalyzer.computeLevel(buffer)

            val bpm = BpmDetector.detect(buffer)
            if (bpm > 0f) {
                bpmState.floatValue = bpm
                bpmConfidence.floatValue = BpmDetector.getConfidence()
            }

            val key = KeyDetector.detect(buffer)
            if (key != null) {
                keyState.value = key
                keyConfidence.floatValue = KeyDetector.getConfidence()
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
