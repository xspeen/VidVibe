package com.vidvibe.app.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data class NeedsConfirmation(val intentSender: IntentSender) : DeleteResult
    data class Failed(val message: String) : DeleteResult
}

class VideoRepository(private val context: Context) {

    private val resolver = context.contentResolver
    private val albumArtUri = Uri.parse("content://media/external/audio/albumart")

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

    suspend fun getAudios(): List<VideoItem> = withContext(Dispatchers.IO) {
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        val tracks = mutableListOf<VideoItem>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn).orEmpty().ifBlank { "Untitled track" }
                    val album = cursor.getString(albumColumn).orEmpty().ifBlank { "Singles" }
                    val albumId = cursor.getLong(albumIdColumn)
                    val sidecar = albumArtFile(name).takeIf { it.isFile }
                    val albumArt = sidecar?.let(Uri::fromFile)
                        ?: if (albumId > 0L) {
                            ContentUris.withAppendedId(albumArtUri, albumId)
                        } else {
                            null
                        }
                    tracks += VideoItem(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ),
                        displayName = name,
                        dateAddedSeconds = cursor.getLong(dateColumn),
                        durationMs = cursor.getLong(durationColumn),
                        bucketName = album,
                        sizeBytes = cursor.getLong(sizeColumn),
                        relativePath = if (relativePathColumn >= 0) {
                            cursor.getString(relativePathColumn).orEmpty()
                        } else {
                            "Music/$album/"
                        },
                        width = 0,
                        height = 0,
                        isAudio = true,
                        artist = cursor.getString(artistColumn).orEmpty(),
                        album = album,
                        albumArtUri = albumArt
                    )
                }
            }
        }
        tracks
    }

    /** Stable sidecar path for the album art of a downloaded track. */
    fun albumArtFile(displayName: String): File {
        val directory = File(context.filesDir, "albumart")
        if (!directory.exists()) directory.mkdirs()
        val key = displayName.lowercase(Locale.US).hashCode().toUInt().toString(16)
        return File(directory, "$key.jpg")
    }

    /** Copies the yt-dlp thumbnail next to the track so the app can show it. */
    fun saveAlbumArt(source: File?, displayName: String): String? {
        if (source == null || !source.isFile || source.length() <= 0L) return null
        val target = albumArtFile(displayName)
        return runCatching {
            source.copyTo(target, overwrite = true)
            target.absolutePath
        }.getOrNull()
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

    suspend fun saveAudioToGallery(
        source: File,
        displayName: String,
        mimeType: String,
        title: String = "",
        artist: String = "",
        album: String = ""
    ): Uri = withContext(Dispatchers.IO) {
        if (!source.exists()) throw IOException("The downloaded file could not be found.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAudioScoped(source, displayName, mimeType, title, artist, album)
        } else {
            saveAudioLegacy(source, displayName, mimeType, title, artist, album)
        }
    }

    private fun saveAudioScoped(
        source: File,
        displayName: String,
        mimeType: String,
        title: String,
        artist: String,
        album: String
    ): Uri {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/VidVibe")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
            if (title.isNotBlank()) put(MediaStore.Audio.Media.TITLE, title)
            if (artist.isNotBlank()) put(MediaStore.Audio.Media.ARTIST, artist)
            if (album.isNotBlank()) put(MediaStore.Audio.Media.ALBUM, album)
            put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, context.packageName)
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Android could not create the music entry.")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Android could not open the music destination.")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null
            )
            return uri
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun saveAudioLegacy(
        source: File,
        displayName: String,
        mimeType: String,
        title: String,
        artist: String,
        album: String
    ): Uri {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "VidVibe"
        )
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create the VidVibe music folder.")
        }
        val target = File(directory, displayName)
        source.copyTo(target, overwrite = true)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.DATA, target.absolutePath)
            put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (title.isNotBlank()) put(MediaStore.Audio.Media.TITLE, title)
            if (artist.isNotBlank()) put(MediaStore.Audio.Media.ARTIST, artist)
            if (album.isNotBlank()) put(MediaStore.Audio.Media.ALBUM, album)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android could not create the music entry.")
        MediaScannerConnection.scanFile(
            context,
            arrayOf(target.absolutePath),
            arrayOf(mimeType),
            null
        )
        return uri
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, context.packageName)
            }
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

    /**
     * Saves a captured frame as a PNG in Pictures/VidVibe and returns its URI.
     */
    suspend fun saveImageToGallery(bitmap: Bitmap, displayName: String): Uri =
        withContext(Dispatchers.IO) {
            val mimeType = "image/png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/VidVibe"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, values)
                    ?: throw IOException("Android could not create the image entry.")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                            throw IOException("The frame could not be encoded.")
                        }
                    } ?: throw IOException("Android could not open the image destination.")
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null
                    )
                    uri
                } catch (error: Throwable) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw error
                }
            } else {
                @Suppress("DEPRECATION")
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ),
                    "VidVibe"
                )
                if (!directory.exists() && !directory.mkdirs()) {
                    throw IOException("Could not create the VidVibe pictures folder.")
                }
                val target = File(directory, displayName)
                FileOutputStream(target).use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        throw IOException("The frame could not be encoded.")
                    }
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.DATA, target.absolutePath)
                    put(
                        MediaStore.Images.Media.DATE_ADDED,
                        System.currentTimeMillis() / 1000L
                    )
                }
                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IOException("Android could not create the image entry.")
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(target.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                uri
            }
        }

    fun mimeTypeFor(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "mp4", "m4v" -> "video/mp4"
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                "m4a", "aac" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                "opus", "ogg", "oga" -> "audio/ogg"
                "flac" -> "audio/flac"
                "wav" -> "audio/wav"
                else -> "video/mp4"
            }
    }
}
