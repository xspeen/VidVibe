package com.vidvibe.app.download

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.vidvibe.app.data.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val queue: List<VideoItem> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val buffering: Boolean = false
) {
    val current: VideoItem? get() = queue.getOrNull(index)
    val hasNext: Boolean get() = index in 0 until queue.lastIndex
    val hasPrevious: Boolean get() = index > 0
}

/**
 * One process-wide audio player. The ExoPlayer lives here instead of inside a
 * composable so playback survives the activity going away, and the MediaSession
 * exposes the queue to the lock screen, notification shade and Bluetooth
 * controls.
 */
object AudioEngine {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var scope: CoroutineScope? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun session(context: Context): MediaSession {
        ensurePlayer(context)
        return session!!
    }

    fun play(context: Context, tracks: List<VideoItem>, index: Int) {
        if (tracks.isEmpty()) return
        val exo = ensurePlayer(context)
        val safeIndex = index.coerceIn(0, tracks.lastIndex)
        _state.value = AudioPlaybackState(
            queue = tracks,
            index = safeIndex,
            durationMs = tracks[safeIndex].durationMs
        )
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.displayName)
                        .setArtist(track.artist.ifBlank { "VidVibe" })
                        .setAlbumTitle(track.album.ifBlank { "Singles" })
                        .setArtworkUri(track.albumArtUri)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        exo.setMediaItems(items, safeIndex, 0L)
        exo.prepare()
        exo.play()
        publish()
        AudioPlaybackService.start(context)
    }

    fun toggle(context: Context) {
        val exo = ensurePlayer(context)
        if (exo.isPlaying) {
            exo.pause()
        } else {
            exo.play()
            AudioPlaybackService.start(context)
        }
        publish()
    }

    fun next(context: Context) {
        val exo = ensurePlayer(context)
        if (exo.hasNextMediaItem()) {
            exo.seekToNextMediaItem()
            exo.play()
            publish()
        }
    }

    fun previous(context: Context) {
        val exo = ensurePlayer(context)
        if (exo.currentPosition > 4_000L) {
            exo.seekTo(0L)
        } else if (exo.hasPreviousMediaItem()) {
            exo.seekToPreviousMediaItem()
            exo.play()
        } else {
            exo.seekTo(0L)
        }
        publish()
    }

    fun seekTo(context: Context, positionMs: Long) {
        val exo = ensurePlayer(context)
        exo.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun playIndex(context: Context, index: Int) {
        val current = _state.value
        if (index !in current.queue.indices) return
        val exo = ensurePlayer(context)
        exo.seekTo(index, 0L)
        exo.play()
        publish()
        AudioPlaybackService.start(context)
    }

    fun stop() {
        player?.pause()
        player?.clearMediaItems()
        _state.value = AudioPlaybackState()
    }

    /** Called by the service when Android tears the process down. */
    fun release() {
        scope?.cancel()
        scope = null
        session?.release()
        session = null
        player?.release()
        player = null
        _state.value = AudioPlaybackState()
    }

    private fun ensurePlayer(context: Context): ExoPlayer {
        player?.let { return it }
        val app = context.applicationContext
        val exo = ExoPlayer.Builder(app).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }
        val mediaSession = MediaSession.Builder(app, exo).build()
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = publish()

            override fun onPlaybackStateChanged(playbackState: Int) = publish()

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) = publish()
        })
        player = exo
        session = mediaSession
        val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = engineScope
        engineScope.launch {
            while (isActive) {
                publish()
                delay(500L)
            }
        }
        return exo
    }

    private fun publish() {
        val exo = player ?: return
        val previous = _state.value
        if (previous.queue.isEmpty()) return
        val index = exo.currentMediaItemIndex.coerceIn(0, previous.queue.lastIndex)
        val duration = exo.duration.takeIf { it > 0L && it != C.TIME_UNSET }
            ?: previous.queue[index].durationMs
        _state.value = previous.copy(
            index = index,
            isPlaying = exo.isPlaying,
            positionMs = exo.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            buffering = exo.playbackState == Player.STATE_BUFFERING
        )
    }
}
