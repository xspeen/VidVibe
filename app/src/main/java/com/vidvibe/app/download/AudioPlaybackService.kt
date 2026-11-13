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

    /**
     * Swiping VidVibe away must never take the music with it. The service is
     * already in the foreground by the time playback starts, and declaring
     * stopWithTask=false keeps the system from tearing it down with the task,
     * so returning here really does leave audio running.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (AudioEngine.state.value.isPlaying) {
            // Re-assert the foreground state: some OEM builds drop it when the
            // task goes away, which is what used to kill playback here. While
            // actually playing, MediaSessionService is guaranteed to answer
            // this with startForeground().
            runCatching { startForegroundService(Intent(this, AudioPlaybackService::class.java)) }
            return
        }
        if (AudioEngine.state.value.hasQueue) {
            // Paused with a queue: stopWithTask=false already keeps the service
            // alive, so leave it running instead of demanding a foreground slot
            // that is not granted while idle.
            return
        }
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // If Android ever kills us, bring the session back rather than leave
        // the queue orphaned without controls.
        return android.app.Service.START_STICKY
    }

    override fun onDestroy() {
        mediaSession = null
        AudioEngine.detachService()
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
