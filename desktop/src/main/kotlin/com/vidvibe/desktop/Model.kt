package com.vidvibe.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Properties

enum class Quality(
    val label: String,
    val selector: String,
    val audioOnly: Boolean = false
) {
    Best("Best", "bv*+ba/b"),
    P1080("1080p", "bv*[height<=1080]+ba/b[height<=1080]"),
    P720("720p", "bv*[height<=720]+ba/b[height<=720]"),
    P480("480p", "bv*[height<=480]+ba/b[height<=480]"),
    Audio("MP3", "ba/b", audioOnly = true)
}

enum class TaskStatus { Queued, Running, Complete, Failed, Cancelled }

class DownloadTask(
    val id: Long,
    val url: String,
    val quality: Quality
) {
    var title by mutableStateOf(url.hostLabel())
    var status by mutableStateOf(TaskStatus.Queued)
    var progress by mutableStateOf(0f)
    var detail by mutableStateOf("Waiting in queue")
    var file by mutableStateOf<File?>(null)
    var error by mutableStateOf<String?>(null)

    val active: Boolean
        get() = status == TaskStatus.Queued || status == TaskStatus.Running
}

enum class Page(val title: String, val subtitle: String) {
    Download("Download", "Paste a link and VidVibe does the rest"),
    Queue("Queue", "Everything running and finished"),
    Library("Library", "Files saved on this PC"),
    Settings("Settings", "Folder, cookies and engine")
}

class Settings(private val file: File) {

    var downloadDir by mutableStateOf(defaultDownloadDir())
    var cookiesBrowser by mutableStateOf("none")
    var closeToTray by mutableStateOf(true)

    fun load() {
        if (!file.isFile) return
        runCatching {
            val properties = Properties()
            file.inputStream().use { properties.load(it) }
            properties.getProperty("downloadDir")?.takeIf { it.isNotBlank() }
                ?.let { downloadDir = File(it) }
            properties.getProperty("cookiesBrowser")?.takeIf { it.isNotBlank() }
                ?.let { cookiesBrowser = it }
            properties.getProperty("closeToTray")?.toBooleanStrictOrNull()
                ?.let { closeToTray = it }
        }
    }

    fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            val properties = Properties()
            properties.setProperty("downloadDir", downloadDir.absolutePath)
            properties.setProperty("cookiesBrowser", cookiesBrowser)
            properties.setProperty("closeToTray", closeToTray.toString())
            file.outputStream().use { properties.store(it, "VidVibe desktop settings") }
        }
    }

    companion object {
        fun defaultDownloadDir(): File {
            val home = File(System.getProperty("user.home"))
            val videos = File(home, "Videos")
            return File(if (videos.isDirectory) videos else home, "VidVibe")
        }
    }
}

private fun String.hostLabel(): String {
    val host = runCatching { java.net.URI(this).host }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
        ?: return "Video"
    return host.split('.').firstOrNull()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Video"
}
