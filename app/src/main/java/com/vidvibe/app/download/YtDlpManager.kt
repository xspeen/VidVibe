package com.vidvibe.app.download

import android.content.Context
import android.os.Environment
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException

interface DownloadProgressCallback {
    fun onProgress(percent: Int, status: String)
}

/** Result of an audio-only download: the track plus its tags and cover. */
data class AudioDownload(
    val file: File,
    val title: String,
    val artist: String,
    val album: String,
    val thumbnail: File?
)

class YtDlpManager(private val context: Context) {

    companion object {
        /** Netscape-format cookies exported from a signed-in browser. */
        fun cookiesFile(context: Context): File = File(context.filesDir, "cookies.txt")

        fun hasCookies(context: Context): Boolean = cookiesFile(context).let {
            it.isFile && it.length() > 0L
        }
    }

    suspend fun downloadVideo(
        url: String,
        qualityHeight: Int,
        onProgress: (Int, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        startPython()

        val outputDirectory = videoDirectory()
        val callback = progressCallback(onProgress)

        val cookiesPath = cookiesFile(context)
            .takeIf { it.isFile && it.length() > 0L }
            ?.absolutePath
            .orEmpty()

        val module = Python.getInstance().getModule("yt_dlp_script")
        val descriptor = callScript(
            module = module,
            function = "download_video",
            url = url,
            qualityHeight = qualityHeight,
            outputDirectory = outputDirectory,
            callback = callback,
            cookiesPath = cookiesPath
        )

        val jobDir = descriptor.optString("job_dir")
            .takeIf { it.isNotBlank() }
            ?.let(::File)

        try {
            if (descriptor.optString("mode") == "merge") {
                val video = File(descriptor.getString("video_path"))
                val audio = File(descriptor.getString("audio_path"))
                val outputName = descriptor.optString("output_name")
                    .ifBlank { "VidVibe_${System.currentTimeMillis()}.mp4" }
                val output = File(outputDirectory, outputName)

                onProgress(98, "Merging video and audio...")
                try {
                    MediaMuxerMerge.merge(video, audio, output)
                    onProgress(100, "Saving to gallery...")
                    return@withContext output
                } catch (mergeError: Throwable) {
                    // The streams use a codec MediaMuxer cannot combine on this
                    // device. Retry with a progressive stream instead.
                    jobDir?.deleteRecursively()
                    onProgress(2, "Downloading a single-stream version...")
                    val fallback = callScript(
                        module = module,
                        function = "download_fallback",
                        url = url,
                        qualityHeight = qualityHeight,
                        outputDirectory = outputDirectory,
                        callback = callback,
                        cookiesPath = cookiesPath
                    )
                    return@withContext finishSingle(fallback, outputDirectory)
                }
            }

            return@withContext finishSingle(descriptor, outputDirectory)
        } finally {
            jobDir?.deleteRecursively()
        }
    }

    /**
     * Downloads the best audio-only stream. Spotify and Apple Music links are
     * resolved to a search query on the Python side, so album art and tags
     * still arrive from the resulting track.
     */
    suspend fun downloadAudio(
        url: String,
        onProgress: (Int, String) -> Unit
    ): AudioDownload = withContext(Dispatchers.IO) {
        startPython()

        val outputDirectory = audioDirectory()
        val callback = progressCallback(onProgress)
        val cookiesPath = cookiesFile(context)
            .takeIf { it.isFile && it.length() > 0L }
            ?.absolutePath
            .orEmpty()

        val module = Python.getInstance().getModule("yt_dlp_script")
        val descriptor = callAudioScript(
            module = module,
            url = url,
            outputDirectory = outputDirectory,
            callback = callback,
            cookiesPath = cookiesPath
        )

        val jobDir = descriptor.optString("job_dir")
            .takeIf { it.isNotBlank() }
            ?.let(::File)

        try {
            val file = finishSingle(descriptor, outputDirectory)
            val thumbnail = descriptor.optString("thumbnail_path")
                .takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.isFile && it.length() > 0L }

            AudioDownload(
                file = file,
                title = descriptor.optString("title").ifBlank { file.nameWithoutExtension },
                artist = descriptor.optString("artist"),
                album = descriptor.optString("album").ifBlank { "Singles" },
                thumbnail = thumbnail
            )
        } finally {
            jobDir?.deleteRecursively()
        }
    }

    private fun startPython() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    private fun progressCallback(onProgress: (Int, String) -> Unit): DownloadProgressCallback =
        object : DownloadProgressCallback {
            override fun onProgress(percent: Int, status: String) {
                onProgress(percent.coerceIn(0, 100), status)
            }
        }

    private fun videoDirectory(): File {
        val outputDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
            "VidVibe"
        )
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IOException("Could not create VidVibe's temporary download folder.")
        }
        return outputDirectory
    }

    private fun audioDirectory(): File {
        val outputDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
            "VidVibe"
        )
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IOException("Could not create VidVibe's temporary music folder.")
        }
        return outputDirectory
    }

    private fun finishSingle(descriptor: JSONObject, outputDirectory: File): File {
        val jobDir = descriptor.optString("job_dir")
            .takeIf { it.isNotBlank() }
            ?.let(::File)

        try {
            val sourcePath = descriptor.optString("file_path")
            if (sourcePath.isBlank()) {
                throw IOException("yt-dlp did not return an output file.")
            }
            val source = File(sourcePath)
            if (!source.exists() || source.length() <= 0L) {
                throw IOException("The downloaded file is empty or missing.")
            }

            val outputName = descriptor.optString("output_name")
                .ifBlank { source.name }
            val target = File(outputDirectory, outputName)

            if (target.exists()) target.delete()
            if (source.absolutePath != target.absolutePath) {
                if (!source.renameTo(target)) {
                    source.copyTo(target, overwrite = true)
                }
            }

            if (!target.exists() || target.length() <= 0L) {
                throw IOException("The downloaded file could not be saved.")
            }
            return target
        } finally {
            jobDir?.deleteRecursively()
        }
    }

    private fun callScript(
        module: PyObject,
        function: String,
        url: String,
        qualityHeight: Int,
        outputDirectory: File,
        callback: DownloadProgressCallback,
        cookiesPath: String
    ): JSONObject {
        try {
            val raw = module.callAttr(
                function,
                url,
                qualityHeight,
                outputDirectory.absolutePath,
                callback,
                cookiesPath
            )?.toJava(String::class.java)
                ?: throw IOException("yt-dlp did not return a result.")

            return JSONObject(raw)
        } catch (error: Throwable) {
            throw IOException(friendlyMessage(error), error)
        }
    }

    private fun callAudioScript(
        module: PyObject,
        url: String,
        outputDirectory: File,
        callback: DownloadProgressCallback,
        cookiesPath: String
    ): JSONObject {
        try {
            val raw = module.callAttr(
                "download_audio",
                url,
                outputDirectory.absolutePath,
                callback,
                cookiesPath
            )?.toJava(String::class.java)
                ?: throw IOException("yt-dlp did not return a result.")

            return JSONObject(raw)
        } catch (error: Throwable) {
            throw IOException(friendlyMessage(error), error)
        }
    }

    private fun friendlyMessage(error: Throwable): String =
        error.message
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.removePrefix("com.chaquo.python.PyException: ")
            ?.takeIf { it.isNotBlank() }
            ?: "The download failed."
}
