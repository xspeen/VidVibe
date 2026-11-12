package com.vidvibe.app.data

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedSeconds: Long,
    val durationMs: Long,
    val bucketName: String,
    val sizeBytes: Long,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val isAudio: Boolean = false,
    val artist: String = "",
    val album: String = "",
    val albumArtUri: Uri? = null
) {
    val durationLabel: String
        get() = formatDuration(durationMs)

    val dateLabel: String
        get() = formatVideoDate(dateAddedSeconds)

    val sizeLabel: String
        get() = formatFileSize(sizeBytes)
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatVideoDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "Unknown date"
    val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    return formatter.format(Date(epochSeconds * 1000L))
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) {
        "${value.toInt()} ${units[index]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[index])
    }
}
