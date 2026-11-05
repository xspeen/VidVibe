package com.vidvibe.app.download

import android.content.Context
import android.os.Environment
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

class YtDlpManager(private val context: Context) {

    suspend fun downloadVideo(
        url: String,
        qualityHeight: Int,
        onProgress: (Int, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val outputDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
            "VidVibe"
        )
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IOException("Could not create VidVibe's temporary download folder.")
        }

        val callback = object : DownloadProgressCallback {
            override fun onProgress(percent: Int, status: String) {
                onProgress(percent.coerceIn(0, 100), status)
            }
        }

        val module = Python.getInstance().getModule("yt_dlp_script")
        val descriptor = callScript(
            module = module,
            function = "download_video",
            url = url,
            qualityHeight = qualityHeight,
            outputDirectory = outputDirectory,
            callback = callback
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
                } catch (mergeError: IOException) {
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
                        callback = callback
                    )
                    return@withContext finishSingle(fallback, outputDirectory)
                }
            }

            return@withContext finishSingle(descriptor, outputDirectory)
        } finally {
            jobDir?.deleteRecursively()
        }
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
        module: com.chaquo.python.PyObject,
        function: String,
        url: String,
        qualityHeight: Int,
        outputDirectory: File,
        callback: DownloadProgressCallback
    ): JSONObject {
        try {
            val raw = module.callAttr(
                function,
                url,
                qualityHeight,
                outputDirectory.absolutePath,
                callback
            )?.toJava(String::class.java)
                ?: throw IOException("yt-dlp did not return a result.")

            return JSONObject(raw)
        } catch (error: Throwable) {
            val message = error.message
                ?.lineSequence()
                ?.firstOrNull { it.isNotBlank() }
                ?.removePrefix("com.chaquo.python.PyException: ")
                ?.takeIf { it.isNotBlank() }
                ?: "The download failed."
            throw IOException(message, error)
        }
    }
}
