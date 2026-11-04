package com.vidvibe.app.download

import android.content.Context
import android.os.Environment
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        try {
            val module = Python.getInstance().getModule("yt_dlp_script")
            val result = module.callAttr(
                "download_video",
                url,
                qualityHeight,
                outputDirectory.absolutePath,
                callback
            )
            val path = result?.toJava(String::class.java)
                ?: throw IOException("yt-dlp did not return an output file.")
            val file = File(path)
            if (!file.exists() || file.length() <= 0L) {
                throw IOException("The downloaded file is empty or missing.")
            }
            file
        } catch (error: Throwable) {
            val message = error.message
                ?.lineSequence()
                ?.firstOrNull { it.isNotBlank() }
                ?.removePrefix("com.chaquo.python.PyException: ")
                ?: "The download failed."
            throw IOException(message, error)
        }
    }
}
