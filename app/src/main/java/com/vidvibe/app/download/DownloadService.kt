package com.vidvibe.app.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Foreground host for [DownloadQueue]. The service exists only while work is
 * queued or running; it stops itself as soon as the queue drains.
 */
class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        DownloadNotifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val active = DownloadQueue.activeTask()
        val notification = DownloadNotifications.buildProgress(
            context = this,
            title = active?.title ?: "VidVibe",
            detail = active?.status ?: "Preparing download...",
            percent = active?.progress ?: 0,
            queued = DownloadQueue.queuedCount()
        )
        startForeground(DownloadNotifications.SERVICE_NOTIFICATION_ID, notification)
        DownloadQueue.consume(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        DownloadNotifications.cancelProgress(this)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stopIfIdle(context: Context) {
            if (DownloadQueue.hasWork) return
            runCatching { context.stopService(Intent(context, DownloadService::class.java)) }
        }
    }
}
