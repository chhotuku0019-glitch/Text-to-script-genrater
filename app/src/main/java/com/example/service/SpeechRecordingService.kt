package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.speech.SpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeechRecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "scriptforge_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_LANGUAGE = "extra_language"

        fun startRecording(context: Context, projectId: Long? = null, language: String = "Hinglish") {
            val intent = Intent(context, SpeechRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_ID, projectId ?: -1L)
                putExtra(EXTRA_LANGUAGE, language)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseRecording(context: Context) {
            val intent = Intent(context, SpeechRecordingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, SpeechRecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, SpeechRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var speechManager: SpeechManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        speechManager = SpeechManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val pId = intent.getLongExtra(EXTRA_PROJECT_ID, -1L)
                val lang = intent.getStringExtra(EXTRA_LANGUAGE) ?: "Hinglish"
                if (pId != -1L) {
                    RecordingStateHolder.setProjectId(pId)
                }
                speechManager?.setLanguage(lang)
                startForegroundRecording()
            }
            ACTION_PAUSE -> {
                pauseRecording()
            }
            ACTION_RESUME -> {
                resumeRecording()
            }
            ACTION_STOP -> {
                stopForegroundRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundRecording() {
        RecordingStateHolder.updateStatus(RecordingStatus.RECORDING)
        speechManager?.startListening()

        val notification = buildNotification(isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimer()
    }

    private fun pauseRecording() {
        RecordingStateHolder.updateStatus(RecordingStatus.PAUSED)
        speechManager?.pauseListening()
        timerJob?.cancel()
        updateNotification(isPaused = true)
    }

    private fun resumeRecording() {
        RecordingStateHolder.updateStatus(RecordingStatus.RECORDING)
        speechManager?.startListening()
        startTimer()
        updateNotification(isPaused = false)
    }

    private fun stopForegroundRecording() {
        timerJob?.cancel()
        speechManager?.stopListening()
        RecordingStateHolder.updateStatus(RecordingStatus.PROCESSING)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && RecordingStateHolder.state.value.status == RecordingStatus.RECORDING) {
                delay(1000)
                val current = RecordingStateHolder.state.value.durationSeconds
                RecordingStateHolder.updateDuration(current + 1)
                updateNotification(isPaused = false)
            }
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(isPaused))
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val durationSec = RecordingStateHolder.state.value.durationSeconds
        val mins = durationSec / 60
        val secs = durationSec % 60
        val timeString = String.format("%02d:%02d", mins, secs)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, SpeechRecordingService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pendingPauseResume = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SpeechRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPaused) "⏸️ ScriptForge AI — Recording Paused" else "🔴 ScriptForge AI — Recording Voice Idea ($timeString)"
        val contentText = if (isPaused) "Tap Resume to continue speaking" else "Listening in background... Tap to open workspace"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pendingPauseResume
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ScriptForge Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active speech recording status and controls for YouTube scripts"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        speechManager?.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
