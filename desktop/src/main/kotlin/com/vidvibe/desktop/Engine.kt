package com.vidvibe.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

data class EngineSetup(
    val ytDlp: File?,
    val ffmpeg: File?,
    val message: String
) {
    val ready: Boolean get() = ytDlp != null
}

/**
 * Owns the two external binaries. They are bundled inside the installer as
 * resources and copied to %LOCALAPPDATA%\VidVibe\bin on first launch, so the
 * app works offline right after setup. If a bundled copy is missing (for
 * example a dev build without the download task) a PATH lookup is used.
 */
object Tools {

    val appDir: File = run {
        val local = System.getenv("LOCALAPPDATA")
            ?: File(System.getProperty("user.home"), "AppData/Local").absolutePath
        File(local, "VidVibe")
    }

    val binDir: File = File(appDir, "bin")

    fun prepare(): EngineSetup {
        binDir.mkdirs()
        val ytDlp = ensureTool("yt-dlp.exe")
        val ffmpeg = ensureTool("ffmpeg.exe")
        ensureTool("ffprobe.exe")
        val message = when {
            ytDlp != null && ffmpeg != null -> "Download engine ready"
            ytDlp != null -> "ffmpeg missing, some formats cannot merge"
            else -> "Download engine not installed"
        }
        return EngineSetup(ytDlp, ffmpeg, message)
    }

    private fun ensureTool(name: String): File? {
        val target = File(binDir, name)
        if (target.isFile && target.length() > 0L) return target
        if (extractBundled(name, target)) return target
        return findOnPath(name)
    }

    private fun extractBundled(name: String, target: File): Boolean {
        val stream = Tools::class.java.getResourceAsStream("/$name") ?: return false
        return runCatching {
            target.parentFile?.mkdirs()
            stream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.length() > 0L
        }.getOrDefault(false)
    }

    fun findOnPath(name: String): File? {
        val path = System.getenv("PATH").orEmpty()
        return path.split(File.pathSeparator)
            .asSequence()
            .filter { it.isNotBlank() }
            .map { File(it, name) }
            .firstOrNull { it.isFile }
    }

    suspend fun install(
        onStatus: (String) -> Unit,
        onProgress: (Float) -> Unit
    ): EngineSetup = withContext(Dispatchers.IO) {
        binDir.mkdirs()

        val ytDlp = File(binDir, "yt-dlp.exe")
        if (!ytDlp.isFile || ytDlp.length() < 1_000_000L) {
            onStatus("Downloading yt-dlp...")
            download(
                "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe",
                ytDlp,
                onProgress
            )
        }

        val ffmpeg = File(binDir, "ffmpeg.exe")
        if (!ffmpeg.isFile || ffmpeg.length() < 1_000_000L) {
            onStatus("Downloading ffmpeg...")
            val zip = File(binDir, "ffmpeg.zip")
            download(
                "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip",
                zip,
                onProgress
            )
            onStatus("Extracting ffmpeg...")
            extractFromZip(zip, listOf("ffmpeg.exe", "ffprobe.exe"), binDir)
            zip.delete()
        }

        onStatus("Checking engine...")
        prepare()
    }

    suspend fun updateYtDlp(onStatus: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val ytDlp = prepare().ytDlp ?: return@withContext "Install the engine first."
        onStatus("Updating yt-dlp...")
        runCatching {
            val process = ProcessBuilder(ytDlp.absolutePath, "-U")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(180, TimeUnit.SECONDS)
            output.lineSequence().lastOrNull { it.isNotBlank() } ?: "yt-dlp is up to date."
        }.getOrElse { "Update failed: ${it.message}" }
    }

    private fun download(url: String, target: File, onProgress: (Float) -> Unit) {
        target.parentFile?.mkdirs()
        var current = URI(url).toURL()
        var redirects = 0
        while (redirects < 8) {
            val connection = current.openConnection()
            connection.setRequestProperty("User-Agent", "VidVibe/2.6")
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            val http = connection as? HttpURLConnection
            val code = http?.responseCode ?: 200
            if (code in 300..399) {
                val location = http?.getHeaderField("Location") ?: break
                current = current.toURI().resolve(location).toURL()
                redirects++
                continue
            }
            if (code >= 400) throw IOException("Server returned HTTP $code")
            val total = connection.contentLengthLong
            var read = 0L
            connection.getInputStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0L) {
                            onProgress((read.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (target.length() < 1_000_000L) throw IOException("Downloaded file is too small")
            onProgress(1f)
            return
        }
        throw IOException("Too many redirects while downloading $url")
    }

    private fun extractFromZip(zip: File, names: List<String>, destination: File) {
        ZipInputStream(zip.inputStream().buffered()).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && name in names) {
                    File(destination, name).outputStream().use { output ->
                        stream.copyTo(output)
                    }
                }
                stream.closeEntry()
            }
        }
    }
}

/**
 * Runs one yt-dlp process. A single instance belongs to one task, so cancelling
 * only ever touches that task's process tree.
 */
class DownloadEngine(private val setup: EngineSetup) {

    @Volatile
    private var process: Process? = null

    suspend fun run(
        task: DownloadTask,
        settings: Settings,
        cookiesBrowser: String?,
        onProgress: (percent: Float, detail: String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val ytDlp = setup.ytDlp
            ?: throw IOException("The download engine is not installed.")
        val outputDir = settings.downloadDir
        if (!outputDir.isDirectory && !outputDir.mkdirs()) {
            throw IOException("Could not create ${outputDir.absolutePath}")
        }

        val command = mutableListOf(
            ytDlp.absolutePath,
            "--newline",
            "--no-playlist",
            "--no-mtime",
            "--progress",
            "--windows-filenames",
            "--print",
            "after_move:VIDVIBE_FILE:%(filepath)s",
            "-o",
            File(outputDir, "%(title).140B [%(id)s].%(ext)s").absolutePath
        )

        setup.ffmpeg?.parentFile?.let { directory ->
            command += listOf("--ffmpeg-location", directory.absolutePath)
        }

        if (task.quality.audioOnly) {
            command += listOf(
                "-f", "ba/b",
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0"
            )
        } else {
            command += listOf(
                "-f", task.quality.selector,
                "--merge-output-format", "mp4"
            )
        }

        if (!cookiesBrowser.isNullOrBlank() && cookiesBrowser != "none") {
            command += listOf("--cookies-from-browser", cookiesBrowser)
        }

        command += task.url

        val started = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        process = started
        started.outputStream.close()

        var finalFile: File? = null
        var lastError: String? = null
        var lastPercent = -1

        started.inputStream.bufferedReader().use { reader ->
            while (true) {
                val raw = reader.readLine() ?: break
                val line = raw.trim()
                if (line.isEmpty()) continue

                if (line.startsWith(FILE_MARKER)) {
                    finalFile = File(line.removePrefix(FILE_MARKER).trim())
                    continue
                }

                val percentMatch = PERCENT.find(line)
                if (percentMatch != null) {
                    val percent = percentMatch.groupValues[1].toFloatOrNull() ?: 0f
                    val whole = percent.toInt()
                    if (whole != lastPercent) {
                        lastPercent = whole
                        val speed = SPEED.find(line)?.groupValues?.get(1)
                        val eta = ETA.find(line)?.groupValues?.get(1)
                        val detail = buildString {
                            append("Downloading")
                            if (!speed.isNullOrBlank()) append("  $speed")
                            if (!eta.isNullOrBlank()) append("  ETA $eta")
                        }
                        onProgress((percent / 100f).coerceIn(0f, 0.99f), detail)
                    }
                    continue
                }

                if (line.startsWith("[download] Destination:")) {
                    val name = line.substringAfter("Destination:").trim()
                    onProgress(task.progress, "Saving ${File(name).name}")
                    continue
                }

                if (line.startsWith("[Merger]")) {
                    onProgress(0.995f, "Merging audio and video...")
                    continue
                }

                if (line.startsWith("ERROR:")) {
                    lastError = line.removePrefix("ERROR:").trim()
                } else if (line.contains("error", ignoreCase = true) && lastError == null) {
                    lastError = line
                }
            }
        }

        val exit = started.waitFor()
        process = null

        if (exit != 0) {
            throw IOException(
                lastError?.substringBefore('\n')?.takeIf { it.isNotBlank() }
                    ?: "yt-dlp exited with code $exit"
            )
        }

        val file = finalFile
            ?: outputDir.listFiles()
                ?.filter { it.isFile }
                ?.maxByOrNull { it.lastModified() }
        if (file == null || !file.isFile) {
            throw IOException("The download finished but no file was found.")
        }
        onProgress(1f, "Saved ${file.name}")
        file
    }

    fun cancel() {
        val running = process ?: return
        runCatching {
            running.descendants().forEach { it.destroyForcibly() }
        }
        runCatching { running.destroyForcibly() }
    }

    private companion object {
        const val FILE_MARKER = "VIDVIBE_FILE:"
        val PERCENT = Regex("""(\d{1,3}(?:\.\d+)?)%""")
        val SPEED = Regex("""at\s+([\d.]+\s*[KMG]i?B/s)""")
        val ETA = Regex("""ETA\s+([\d:]+)""")
    }
}

fun detectBrowserCookies(): String? {
    val local = System.getenv("LOCALAPPDATA") ?: return null
    val roaming = System.getenv("APPDATA").orEmpty()

    val chrome = File(local, "Google/Chrome/User Data")
    if (chrome.isDirectory) return "chrome"

    val edge = File(local, "Microsoft/Edge/User Data")
    if (edge.isDirectory) return "edge"

    val brave = File(local, "BraveSoftware/Brave-Browser/User Data")
    if (brave.isDirectory) return "brave"

    val firefox = File(roaming, "Mozilla/Firefox/Profiles")
    if (firefox.isDirectory) return "firefox"

    return null
}

fun looksLikeCookieError(message: String?): Boolean {
    val text = message.orEmpty().lowercase()
    return listOf(
        "sign in",
        "log in",
        "login",
        "cookies",
        "private",
        "members-only",
        "age-restricted",
        "age restricted",
        "http error 401",
        "http error 403"
    ).any { text.contains(it) }
}
