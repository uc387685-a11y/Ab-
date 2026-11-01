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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.voice.SpeechManager

class WakeWordService : Service() {
    private val TAG = "WakeWordService"
    private var speechManager: SpeechManager? = null

    companion object {
        const val CHANNEL_ID = "hello_umesh_wakeword_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP"
        const val ACTION_WAKE_WORD_HEARD = "com.example.service.ACTION_WAKE_WORD_HEARD"
        const val EXTRA_WAKE_PHRASE = "extra_wake_phrase"

        var isServiceRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isServiceRunning = true
        initWakeRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        restartListening()
        return START_STICKY
    }

    private fun initWakeRecognizer() {
        speechManager = SpeechManager(this).apply {
            setHandsFreeMode(true)
            onWakeWordDetected = { phrase, command ->
                Log.d(TAG, "Wake phrase detected in background service: $phrase, command: $command")
                val broadcastIntent = Intent(ACTION_WAKE_WORD_HEARD).apply {
                    putExtra(EXTRA_WAKE_PHRASE, phrase)
                    if (command.isNotBlank()) {
                        putExtra("extra_command", command)
                    }
                    setPackage(packageName)
                }
                sendBroadcast(broadcastIntent)

                // Launch MainActivity to respond immediately
                val openIntent = Intent(this@WakeWordService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(EXTRA_WAKE_PHRASE, phrase)
                    if (command.isNotBlank()) {
                        putExtra("extra_command", command)
                    }
                }
                startActivity(openIntent)
            }
            onErrorOccurred = {
                // In background on aggressive OEM ROMs (like MIUI/HyperOS), restarts listening gracefully
                restartListening()
            }
        }
    }

    private fun restartListening() {
        try {
            speechManager?.startListening("hi-IN")
        } catch (e: Exception) {
            Log.w(TAG, "Could not restart background speech listener", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hands-Free Wake Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status while listening hands-free for 'Hello Maayra'"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Maayra AI सुन रही है...")
            .setContentText("बोलिए 'Hello Maayra' या 'हाँ उमेश'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Listening", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isServiceRunning = false
        speechManager?.destroy()
        speechManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
