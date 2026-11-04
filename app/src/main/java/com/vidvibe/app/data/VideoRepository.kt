package com.vidvibe.app.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data class NeedsConfirmation(val intentSender: IntentSender) : DeleteResult
    data class Failed(val message: String) : DeleteResult
}

class VideoRepository(private val context: Context) {

    private val resolver = context.contentResolver

    suspend fun getVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Video.Media.RELATIVE_PATH)
        }

        val videos = mutableListOf<VideoItem>()
        runCatching {
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val bucket = cursor.getString(bucketColumn).orEmpty().ifBlank { "Videos" }
                    val relativePath = if (relativePathColumn >= 0) {
                        cursor.getString(relativePathColumn).orEmpty()
                    } else {
                        "Movies/$bucket/"
                    }
                    videos += VideoItem(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        ),
                        displayName = cursor.getString(nameColumn).orEmpty().ifBlank { "Untitled video" },
                        dateAddedSeconds = cursor.getLong(dateColumn),
                        durationMs = cursor.getLong(durationColumn),
                        bucketName = bucket,
                        sizeBytes = cursor.getLong(sizeColumn),
                        relativePath = relativePath,
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn)
                    )
                }
            }
        }
        videos
    }

    suspend fun deleteVideos(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Deleted

        val needsPermission = mutableListOf<Uri>()
        val failed = mutableListOf<Uri>()
        var failureMessage: String? = null
        var securityException: SecurityException? = null

        uris.forEach { uri ->
            try {
                resolver.delete(uri, null, null)
            } catch (security: SecurityException) {
                needsPermission += uri
                if (securityException == null) securityException = security
            } catch (error: Throwable) {
                failed += uri
                if (failureMessage == null) {
                    failureMessage = error.message ?: "Could not delete the selected video."
                }
            }
        }

        if (needsPermission.isEmpty()) {
            return@withContext if (failed.isEmpty()) {
                DeleteResult.Deleted
            } else {
                DeleteResult.Failed(
                    failureMessage ?: "Some videos could not be deleted."
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, needsPermission)
                DeleteResult.NeedsConfirmation(pendingIntent.intentSender)
            }.getOrElse {
                DeleteResult.Failed(
                    it.message ?: "Android could not request deletion permission."
                )
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val recoverable = securityException as? RecoverableSecurityException
            if (recoverable != null) {
                DeleteResult.NeedsConfirmation(
                    recoverable.userAction.actionIntent.intentSender
                )
            } else {
                DeleteResult.Failed(
                    securityException?.message ?: "Deletion permission was denied."
                )
            }
        } else {
            DeleteResult.Failed(
                securityException?.message ?: "Deletion permission was denied."
            )
        }
    }

    suspend fun saveToGallery(
        source: File,
        displayName: String,
        mimeType: String
    ): Uri = withContext(Dispatchers.IO) {
        if (!source.exists()) throw IOException("The downloaded file could not be found.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(source, displayName, mimeType)
        } else {
            saveLegacy(source, displayName, mimeType)
        }
    }

    private fun saveScoped(source: File, displayName: String, mimeType: String): Uri {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/VidVibe"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("Android could not create the gallery entry.")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Android could not open the gallery destination.")

            val publishValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
            return uri
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(source: File, displayName: String, mimeType: String): Uri {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "VidVibe"
        )
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create the VidVibe folder.")
        }
        val target = File(directory, displayName)
        source.copyTo(target, overwrite = true)

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            put(MediaStore.Video.Media.DATA, target.absolutePath)
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android could not create the gallery entry.")
        MediaScannerConnection.scanFile(
            context,
            arrayOf(target.absolutePath),
            arrayOf(mimeType),
            null
        )
        return uri
    }

    fun mimeTypeFor(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "mp4", "m4v" -> "video/mp4"
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                else -> "video/mp4"
            }
    }
}
