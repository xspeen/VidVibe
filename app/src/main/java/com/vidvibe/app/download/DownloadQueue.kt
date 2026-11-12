package com.vidvibe.app.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.vidvibe.app.data.Platform
import com.vidvibe.app.data.SettingsStore
import com.vidvibe.app.data.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

enum class QueueStatus { Queued, Running, Complete, Failed, Cancelled }

data class DownloadTask(
    val id: Long,
    val url: String,
    val qualityHeight: Int,
    val title: String,
    val progress: Int = 0,
    val status: String = "Queued",
    val state: QueueStatus = QueueStatus.Queued,
    val fileName: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isAudio: Boolean = false,
    val platform: Platform = Platform.Other,
    val artist: String = "",
    val album: String = "",
    val albumArtPath: String? = null,
    val completedAt: Long? = null
) {
    val isFinished: Boolean
        get() = state == QueueStatus.Complete ||
            state == QueueStatus.Failed ||
            state == QueueStatus.Cancelled
}

/**
 * Process-wide download queue. A foreground service keeps this object alive
 * while work is pending, so downloads continue when the activity, the share
 * drawer, or the whole UI is gone. The UI only observes [tasks].
 */
object DownloadQueue {

    /** Finished cards linger briefly so the in-bar tick can play, then vanish. */
    private const val AUTO_REMOVE_MS = 6200L

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val counter = AtomicLong(0L)
    private val running = AtomicBoolean(false)
    private val cancelRequests: MutableSet<Long> =
        Collections.synchronizedSet(mutableSetOf())

    private var worker: CoroutineScope? = null
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val hasWork: Boolean
        get() = _tasks.value.any {
            it.state == QueueStatus.Queued || it.state == QueueStatus.Running
        }

    fun activeTask(): DownloadTask? =
        _tasks.value.firstOrNull { it.state == QueueStatus.Running }
            ?: _tasks.value.firstOrNull { it.state == QueueStatus.Queued }

    fun queuedCount(): Int = _tasks.value.count { it.state == QueueStatus.Queued }

    fun enqueue(
        context: Context,
        url: String,
        qualityHeight: Int,
        isAudio: Boolean = false
    ): Long {
        val id = counter.incrementAndGet()
        val platform = Platform.fromUrl(url)
        val task = DownloadTask(
            id = id,
            url = url.trim(),
            qualityHeight = qualityHeight,
            title = titleFor(url, platform, isAudio),
            isAudio = isAudio || platform.audio,
            platform = platform
        )
        _tasks.update { it + task }
        DownloadService.start(context)
        return id
    }

    fun retry(context: Context, id: Long) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        if (task.state != QueueStatus.Failed && task.state != QueueStatus.Cancelled) return
        cancelRequests.remove(id)
        _tasks.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        state = QueueStatus.Queued,
                        progress = 0,
                        status = "Queued",
                        error = null,
                        fileName = null,
                        completedAt = null
                    )
                } else {
                    it
                }
            }
        }
        DownloadService.start(context)
    }

    fun cancel(context: Context, id: Long) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        cancelRequests.add(id)
        if (task.state == QueueStatus.Queued) {
            _tasks.update { list ->
                list.map {
                    if (it.id == id) {
                        it.copy(state = QueueStatus.Cancelled, status = "Cancelled")
                    } else {
                        it
                    }
                }
            }
            DownloadService.stopIfIdle(context)
        }
    }

    fun remove(id: Long) {
        cancelRequests.remove(id)
        _tasks.update { list ->
            list.filterNot {
                it.id == id && it.isFinished
            }
        }
    }

    fun clearFinished() {
        _tasks.update { list ->
            list.filter {
                it.state == QueueStatus.Queued || it.state == QueueStatus.Running
            }
        }
    }

    /**
     * Starts the sequential worker. Safe to call repeatedly; only one worker
     * ever runs at a time and a second call returns immediately.
     */
    fun consume(context: Context) {
        if (!running.compareAndSet(false, true)) return
        val app = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        worker = scope
        scope.launch {
            try {
                while (true) {
                    val next = _tasks.value.firstOrNull { it.state == QueueStatus.Queued }
                        ?: break
                    process(app, next.id)
                }
            } finally {
                worker = null
                running.set(false)
                // Work can be enqueued in the window between the loop's last
                // check and the flag reset. Re-check before tearing the
                // service down so a fresh task is never stranded in the queue
                // with nothing consuming it.
                if (hasWork) {
                    DownloadService.start(app)
                } else {
                    DownloadService.stopIfIdle(app)
                }
            }
        }
    }

    private suspend fun process(context: Context, id: Long) {
        val start = _tasks.value.firstOrNull { it.id == id } ?: return
        if (start.state != QueueStatus.Queued) return

        update(id) {
            it.copy(state = QueueStatus.Running, progress = 0, status = "Preparing download...")
        }
        DownloadNotifications.postProgress(
            context,
            start.title,
            "Preparing download...",
            0,
            queuedCount()
        )

        val manager = YtDlpManager(context)
        val repository = VideoRepository(context)

        try {
            val settings = SettingsStore(context).settings.first()
            if (settings.wifiOnly && !isOnWifi(context)) {
                throw IOException(
                    "Wi-Fi only is enabled. Connect to Wi-Fi or turn the option off."
                )
            }

            var lastPercent = -1
            val onProgress: (Int, String) -> Unit = { percent, status ->
                if (id !in cancelRequests) {
                    update(id) { it.copy(progress = percent, status = status) }
                    if (percent != lastPercent) {
                        lastPercent = percent
                        DownloadNotifications.postProgress(
                            context,
                            start.title,
                            status,
                            percent,
                            queuedCount()
                        )
                    }
                }
            }

            if (start.isAudio) {
                processAudio(context, id, start, manager, repository, onProgress)
            } else {
                processVideo(context, id, start, manager, repository, onProgress)
            }
        } catch (error: Throwable) {
            val message = error.message
                ?.substringBefore('\n')
                ?.takeIf { it.isNotBlank() }
                ?: "The download could not be completed."
            if (id in cancelRequests) {
                update(id) {
                    it.copy(state = QueueStatus.Cancelled, status = "Cancelled")
                }
            } else {
                update(id) {
                    it.copy(state = QueueStatus.Failed, status = "Failed", error = message)
                }
                val failed = _tasks.value.firstOrNull { it.id == id }
                if (failed != null) DownloadNotifications.notifyFailed(context, failed)
            }
        } finally {
            cancelRequests.remove(id)
        }
    }

    private suspend fun processVideo(
        context: Context,
        id: Long,
        start: DownloadTask,
        manager: YtDlpManager,
        repository: VideoRepository,
        onProgress: (Int, String) -> Unit
    ) {
        val file = manager.downloadVideo(start.url, start.qualityHeight) { percent, status ->
            onProgress(percent, status)
        }

        if (id in cancelRequests) {
            runCatching { file.delete() }
            update(id) { it.copy(state = QueueStatus.Cancelled, status = "Cancelled") }
            return
        }

        val galleryName = if (file.name.startsWith("VidVibe_")) {
            file.name
        } else {
            "VidVibe_${file.name}"
        }
        update(id) { it.copy(progress = 99, status = "Saving to gallery...") }
        repository.saveToGallery(file, galleryName, repository.mimeTypeFor(galleryName))
        runCatching { file.delete() }

        complete(id, galleryName)
    }

    private suspend fun processAudio(
        context: Context,
        id: Long,
        start: DownloadTask,
        manager: YtDlpManager,
        repository: VideoRepository,
        onProgress: (Int, String) -> Unit
    ) {
        val audio = manager.downloadAudio(start.url) { percent, status ->
            onProgress(percent, status)
        }

        if (id in cancelRequests) {
            runCatching { audio.file.delete() }
            runCatching { audio.thumbnail?.delete() }
            update(id) { it.copy(state = QueueStatus.Cancelled, status = "Cancelled") }
            return
        }

        val title = audio.title.ifBlank { start.title }
        val extension = audio.file.extension.ifBlank { "m4a" }
        val base = title
            .replace(Regex("[^A-Za-z0-9._ -]+"), "_")
            .trim(' ', '.', '_')
            .take(70)
            .ifBlank { "VidVibe_${System.currentTimeMillis()}" }
        val galleryName = "$base.$extension"

        update(id) { it.copy(progress = 99, status = "Saving to your music...") }
        repository.saveAudioToGallery(
            audio.file,
            galleryName,
            repository.mimeTypeFor(galleryName),
            title = title,
            artist = audio.artist,
            album = audio.album
        )
        val artPath = repository.saveAlbumArt(audio.thumbnail, galleryName)
        runCatching { audio.file.delete() }
        runCatching { audio.thumbnail?.delete() }

        complete(
            id = id,
            fileName = galleryName,
            title = title,
            artist = audio.artist,
            album = audio.album,
            albumArtPath = artPath
        )
    }

    private fun complete(
        id: Long,
        fileName: String,
        title: String? = null,
        artist: String = "",
        album: String = "",
        albumArtPath: String? = null
    ) {
        val finishedAt = System.currentTimeMillis()
        update(id) {
            it.copy(
                state = QueueStatus.Complete,
                progress = 100,
                status = "Saved",
                fileName = fileName,
                title = title?.takeIf { value -> value.isNotBlank() } ?: it.title,
                artist = artist,
                album = album,
                albumArtPath = albumArtPath,
                completedAt = finishedAt
            )
        }
        cleanupScope.launch {
            delay(AUTO_REMOVE_MS)
            val task = _tasks.value.firstOrNull { it.id == id } ?: return@launch
            if (task.state == QueueStatus.Complete && task.completedAt == finishedAt) {
                remove(id)
            }
        }
    }

    private fun update(id: Long, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    private fun isOnWifi(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun titleFor(url: String, platform: Platform, isAudio: Boolean): String {
        if (platform != Platform.Other) {
            return if (isAudio || platform.audio) {
                "${platform.label} audio"
            } else {
                "${platform.label} video"
            }
        }
        val host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
            .removePrefix("www.")
        return host.ifBlank { if (isAudio) "Audio" else "Video" }
            .split('.')
            .firstOrNull()
            ?.replaceFirstChar { it.uppercase() }
            ?: if (isAudio) "Audio" else "Video"
    }
}
