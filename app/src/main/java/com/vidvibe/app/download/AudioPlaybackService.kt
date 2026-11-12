package com.vidvibe.app.download

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Hosts the process-wide audio player in the foreground so music keeps playing
 * after VidVibe is closed. The MediaSession supplies the lock-screen and
 * notification-shade controls, including the queue.
 */
class AudioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = AudioEngine.session(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (AudioEngine.state.value.isPlaying) {
            // Keep the foreground service alive so audio continues in the
            // background, exactly like a music app.
            return
        }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession = null
        AudioEngine.release()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, AudioPlaybackService::class.java))
            }
        }
    }
}
