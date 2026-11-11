package com.vidvibe.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * All window state lives here. Downloads are sequential so progress stays
 * readable and a single slow host never starves the rest of the queue.
 */
class AppState {

    val settings = Settings(File(Tools.appDir, "settings.properties")).also { it.load() }
    val tasks = mutableStateListOf<DownloadTask>()

    var engine by mutableStateOf<EngineSetup?>(null)
    var installing by mutableStateOf(false)
    var installStatus by mutableStateOf("")
    var installProgress by mutableStateOf(0f)
    var page by mutableStateOf(Page.Download)
    var toast by mutableStateOf<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val counter = AtomicLong(0L)
    private var worker: Job? = null
    private var activeEngine: DownloadEngine? = null

    val activeCount: Int get() = tasks.count { it.active }

    init {
        refreshEngine()
    }

    fun showToast(message: String) {
        toast = message
    }

    fun refreshEngine() {
        scope.launch {
            engine = withContext(Dispatchers.IO) { Tools.prepare() }
        }
    }

    fun installEngine() {
        if (installing) return
        installing = true
        installProgress = 0f
        installStatus = "Starting..."
        scope.launch {
            val result = runCatching {
                Tools.install(
                    onStatus = { installStatus = it },
                    onProgress = { installProgress = it }
                )
            }
            installing = false
            result.onSuccess {
                engine = it
                showToast(it.message)
            }.onFailure {
                showToast(it.message ?: "Engine install failed")
            }
        }
    }

    fun updateEngine() {
        if (installing) return
        installing = true
        installProgress = 0f
        scope.launch {
            val message = Tools.updateYtDlp { installStatus = it }
            installing = false
            installStatus = message
            refreshEngine()
            showToast(message)
        }
    }

    fun enqueue(url: String, quality: Quality): Boolean {
        val clean = url.trim()
        val valid = runCatching {
            val uri = java.net.URI(clean)
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
        if (!valid) {
            showToast("Paste a valid http or https link.")
            return false
        }
        if (engine?.ready != true) {
            showToast("Install the download engine first.")
            page = Page.Settings
            return false
        }

        tasks.add(DownloadTask(counter.incrementAndGet(), clean, quality))
        startWorker()
        return true
    }

    fun cancel(task: DownloadTask) {
        when (task.status) {
            TaskStatus.Running -> {
                task.detail = "Cancelling..."
                task.status = TaskStatus.Cancelled
                activeEngine?.cancel()
            }

            TaskStatus.Queued -> {
                task.status = TaskStatus.Cancelled
                task.detail = "Cancelled"
            }

            else -> Unit
        }
    }

    fun retry(task: DownloadTask) {
        if (task.active) return
        task.status = TaskStatus.Queued
        task.progress = 0f
        task.detail = "Waiting in queue"
        task.error = null
        task.file = null
        startWorker()
    }

    fun remove(task: DownloadTask) {
        if (task.active) cancel(task)
        tasks.remove(task)
    }

    fun clearFinished() {
        tasks.removeAll { !it.active }
    }

    fun quit() {
        activeEngine?.cancel()
        scope.cancel()
    }

    private fun startWorker() {
        if (worker?.isActive == true) return
        worker = scope.launch {
            while (true) {
                val next = tasks.firstOrNull { it.status == TaskStatus.Queued } ?: break
                runTask(next)
            }
        }
    }

    private suspend fun runTask(task: DownloadTask) {
        val setup = engine
        if (setup?.ytDlp == null) {
            task.status = TaskStatus.Failed
            task.error = "The download engine is not installed."
            return
        }

        task.status = TaskStatus.Running
        task.progress = 0f
        task.detail = "Preparing download..."
        task.error = null

        val downloader = DownloadEngine(setup)
        activeEngine = downloader

        try {
            val onProgress: (Float, String) -> Unit = { percent, detail ->
                task.progress = percent.coerceIn(0f, 1f)
                task.detail = detail
            }

            val requestedBrowser = settings.cookiesBrowser.takeIf { it != "none" }
            val file = try {
                downloader.run(task, settings, requestedBrowser, onProgress)
            } catch (error: Throwable) {
                if (task.status == TaskStatus.Cancelled) throw error
                val detected = if (requestedBrowser == null) {
                    detectBrowserCookies()
                } else {
                    null
                }
                if (detected != null && looksLikeCookieError(error.message)) {
                    task.detail = "Retrying with $detected cookies..."
                    downloader.run(task, settings, detected, onProgress)
                } else {
                    throw error
                }
            }

            task.file = file
            task.title = file.nameWithoutExtension
            task.status = TaskStatus.Complete
            task.progress = 1f
            task.detail = "Saved to ${settings.downloadDir.name}"
            showToast("Downloaded ${file.name}")
        } catch (error: Throwable) {
            if (task.status == TaskStatus.Cancelled) {
                task.detail = "Cancelled"
            } else {
                task.status = TaskStatus.Failed
                task.error = error.message
                    ?.lineSequence()
                    ?.firstOrNull { it.isNotBlank() }
                    ?: "The download failed."
                task.detail = "Failed"
            }
        } finally {
            activeEngine = null
        }
    }
}

fun scanLibrary(directory: File): List<File> {
    if (!directory.isDirectory) return emptyList()
    val extensions = setOf(
        "mp4", "mkv", "webm", "mov", "avi", "m4v",
        "mp3", "m4a", "opus", "ogg", "flac", "wav"
    )
    return directory.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in extensions }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()
}
