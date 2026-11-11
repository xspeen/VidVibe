package com.vidvibe.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vidvibe.app.MainActivity
import com.vidvibe.app.R

/**
 * Bridge between the playback notification and the player composable. The
 * player registers its pause/stop lambdas while it is alive; the notification
 * actions call them without needing a bound service.
 */
object PlaybackBridge {
    var onPause: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null
}

/**
 * Keeps audio playing while VidVibe is in the background. The service itself
 * owns no player: it only holds the process in the foreground with a media
 * playback type so the existing ExoPlayer keeps running after the activity
 * stops. It is started when the user turns on background audio, never from the
 * background, which keeps Android 12+ foreground-service rules happy.
 */
class AudioPlaybackService : Service() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                PlaybackBridge.onPause?.invoke()
                updateNotification(paused = true)
                return START_NOT_STICKY
            }

            ACTION_STOP -> {
                PlaybackBridge.onStop?.invoke()
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() }
                    ?: "VidVibe"
                startForeground(NOTIFICATION_ID, buildNotification(title, paused = false))
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun updateNotification(paused: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return
        manager.notify(
            NOTIFICATION_ID,
            buildNotification("VidVibe", paused)
        )
    }

    private fun buildNotification(title: String, paused: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioPlaybackService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AudioPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_window)
            .setContentTitle(if (paused) "Playback paused" else "Playing in background")
            .setContentText(title)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Pause", pauseIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background audio",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps video audio playing when VidVibe is in the background."
            setShowBadge(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "vidvibe_playback"
        private const val NOTIFICATION_ID = 4202
        private const val ACTION_PAUSE = "com.vidvibe.app.action.PAUSE_PLAYBACK"
        private const val ACTION_STOP = "com.vidvibe.app.action.STOP_PLAYBACK"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, title: String) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = "com.vidvibe.app.action.START_PLAYBACK"
                putExtra(EXTRA_TITLE, title)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, AudioPlaybackService::class.java))
            }
        }
    }
}
