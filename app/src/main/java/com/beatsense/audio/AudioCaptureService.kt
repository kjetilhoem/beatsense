package com.beatsense.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import kotlin.concurrent.thread

class AudioCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "beatsense_capture"

        const val MODE_APP_AUDIO = "app_audio"
        const val MODE_MICROPHONE = "microphone"

        var onAudioData: ((FloatArray) -> Unit)? = null
    }

    private val sampleRate = AudioConfig.SAMPLE_RATE
    private val bufferSize = AudioConfig.BUFFER_SIZE

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra("mode") ?: MODE_APP_AUDIO

        createNotificationChannel()
        val notificationText = if (mode == MODE_MICROPHONE) "Listening to microphone..." else "Analyzing app audio..."
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BeatSense")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        val serviceType = if (mode == MODE_MICROPHONE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        }
        startForeground(1, notification, serviceType)

        if (mode == MODE_MICROPHONE) {
            startMicCapture()
        } else {
            val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
            val data = intent?.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)
            startPlaybackCapture()
        }

        return START_NOT_STICKY
    }

    private fun startPlaybackCapture() {
        val projection = mediaProjection ?: return

        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(captureConfig)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize * 4)
            .build()

        startRecordingLoop()
    }

    private fun startMicCapture() {
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(maxOf(bufferSize * 4, minBufferSize))
            .build()

        startRecordingLoop()
    }

    private fun startRecordingLoop() {
        audioRecord?.startRecording()
        isRecording = true

        thread(name = "AudioCapture") {
            val buffer = FloatArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize, AudioRecord.READ_BLOCKING) ?: 0
                if (read > 0) {
                    onAudioData?.invoke(buffer.copyOf(read))
                }
            }
        }
    }

    override fun onDestroy() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Capture",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
