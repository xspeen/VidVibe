package com.vidvibe.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vidvibe.app.MainActivity
import com.vidvibe.app.R

/**
 * All notification surfaces for downloads. Channels are created once, and every
 * post is permission-safe so a denied POST_NOTIFICATIONS never crashes a
 * background download.
 */
object DownloadNotifications {

    const val CHANNEL_DOWNLOADS = "vidvibe_downloads"
    const val CHANNEL_UPDATES = "vidvibe_updates"

    const val SERVICE_NOTIFICATION_ID = 4101
    const val WELCOME_NOTIFICATION_ID = 4102
    private const val DONE_BASE = 4200

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val downloads = NotificationChannel(
            CHANNEL_DOWNLOADS,
            "Background downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live progress for downloads that keep running in the background."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val updates = NotificationChannel(
            CHANNEL_UPDATES,
            "VidVibe updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Finished downloads and VidVibe announcements."
            enableVibration(true)
        }

        manager.createNotificationChannel(downloads)
        manager.createNotificationChannel(updates)
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DOWNLOADS, true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildProgress(
        context: Context,
        title: String,
        detail: String,
        percent: Int,
        queued: Int = 0
    ): Notification {
        val summary = if (queued > 0) "$detail  •  $queued queued" else detail
        return NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_stat_window)
            .setContentTitle(title)
            .setContentText(summary)
            .setContentIntent(contentIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setColor(0xFF4E9BFF.toInt())
            .setProgress(100, percent.coerceIn(0, 100), percent <= 0)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun postProgress(
        context: Context,
        title: String,
        detail: String,
        percent: Int,
        queued: Int = 0
    ) {
        if (!canPost(context)) return
        runCatching {
            NotificationManagerCompat.from(context).notify(
                SERVICE_NOTIFICATION_ID,
                buildProgress(context, title, detail, percent, queued)
            )
        }
    }

    fun notifyComplete(context: Context, task: DownloadTask) {
        if (!canPost(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_window)
            .setContentTitle("Saved to your gallery")
            .setContentText(task.fileName ?: task.title)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setColor(0xFF30D158.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.fileName ?: task.title))
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(
                DONE_BASE + task.id.toInt(),
                notification
            )
        }
    }

    fun notifyFailed(context: Context, task: DownloadTask) {
        if (!canPost(context)) return
        val message = task.error ?: "The download could not be completed."
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_window)
            .setContentTitle("Download stopped")
            .setContentText(message)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setColor(0xFFFF453A.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(
                DONE_BASE + 500 + task.id.toInt(),
                notification
            )
        }
    }

    /**
     * First-run announcement. The custom layout carries an indeterminate
     * shimmer bar, so the notification itself visibly moves when it drops in.
     */
    fun showWelcome(context: Context) {
        if (!canPost(context)) return
        val views = RemoteViews(context.packageName, R.layout.notification_welcome)
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_window)
            .setContentTitle("VidVibe is ready")
            .setContentText("Background downloads are on. You can leave the app while a video saves.")
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF4E9BFF.toInt())
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(WELCOME_NOTIFICATION_ID, notification)
        }
    }

    fun cancelProgress(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(SERVICE_NOTIFICATION_ID) }
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    const val EXTRA_OPEN_DOWNLOADS = "vidvibe_open_downloads"
}
