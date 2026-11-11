package com.vidvibe.app.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale

sealed interface TrimResult {
    data class Success(
        val uri: Uri,
        val displayName: String,
        val durationMs: Long
    ) : TrimResult

    data class Failure(val message: String) : TrimResult
}

/**
 * Lossless trim. The selected range is copied straight out of the source
 * container with [MediaMuxer], so the clip keeps the original bitrate and no
 * frame is re-encoded. The start is snapped to the nearest keyframe before the
 * chosen point, which is what makes a stream copy possible at all.
 */
class TrimExporter(private val context: Context) {

    suspend fun export(
        source: Uri,
        startMs: Long,
        endMs: Long,
        displayName: String,
        onProgress: (Float) -> Unit = {}
    ): TrimResult = withContext(Dispatchers.IO) {
        if (endMs - startMs < MIN_RANGE_MS) {
            return@withContext TrimResult.Failure(
                "Pick a range of at least ${MIN_RANGE_MS / 1000} seconds."
            )
        }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var output: File? = null
        try {
            runCatching { extractor.setDataSource(context, source, null) }
                .getOrElse {
                    return@withContext TrimResult.Failure(
                        "This video could not be opened for trimming."
                    )
                }

            val videoTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    ?.startsWith("video/") == true
            } ?: return@withContext TrimResult.Failure(
                "This file has no video track to trim."
            )

            val videoFormat = extractor.getTrackFormat(videoTrack)
            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            val container = when {
                videoMime in MP4_VIDEO_MIMES -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                videoMime in WEBM_VIDEO_MIMES -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                else -> return@withContext TrimResult.Failure(
                    "This video codec cannot be trimmed without re-encoding."
                )
            }

            val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
                val mime = extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    .orEmpty()
                when (container) {
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 ->
                        mime in MP4_AUDIO_MIMES

                    else -> mime in WEBM_AUDIO_MIMES
                }
            }

            extractor.selectTrack(videoTrack)
            if (audioTrack != null) extractor.selectTrack(audioTrack)

            val rotation = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, source)
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )?.toIntOrNull() ?: 0
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrDefault(0)

            val extension = if (
                container == MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            ) "webm" else "mp4"
            val temp = File(
                context.cacheDir,
                "trim_${System.currentTimeMillis()}.$extension"
            )
            output = temp

            val newMuxer = MediaMuxer(
                temp.absolutePath,
                container
            )
            muxer = newMuxer
            if (
                container == MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 &&
                rotation != 0
            ) {
                runCatching { newMuxer.setOrientationHint(rotation) }
            }

            val videoOut = newMuxer.addTrack(videoFormat)
            if (audioTrack != null) {
                newMuxer.addTrack(extractor.getTrackFormat(audioTrack))
            }
            newMuxer.start()

            extractor.seekTo(
                startMs * 1000L,
                MediaExtractor.SEEK_TO_PREVIOUS_SYNC
            )

            val buffer = ByteBuffer.allocateDirect(BUFFER_BYTES)
            val info = MediaCodec.BufferInfo()
            val rangeUs = ((endMs - startMs) * 1000L).coerceAtLeast(1L)
            var firstUs = -1L
            var written = 0L
            var lastReported = -1

            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val timeUs = extractor.sampleTime
                if (timeUs < 0L) break
                if (timeUs > endMs * 1000L) break

                if (firstUs < 0L) firstUs = timeUs
                val relative = (timeUs - firstUs).coerceAtLeast(0L)

                info.offset = 0
                info.size = size
                info.presentationTimeUs = relative
                info.flags = extractor.sampleFlags

                // Only the video track was mapped to a muxer index above; the
                // audio track sits right after it when present.
                val target = if (extractor.sampleTrackIndex == videoTrack) {
                    videoOut
                } else {
                    videoOut + 1
                }
                runCatching { newMuxer.writeSampleData(target, buffer, info) }
                written++
                extractor.advance()

                val percent = (
                    (timeUs - startMs * 1000L).toFloat() / rangeUs.toFloat() * 100f
                    ).toInt().coerceIn(0, 100)
                if (percent != lastReported) {
                    lastReported = percent
                    onProgress(percent / 100f)
                }
            }

            newMuxer.stop()
            muxer = null

            if (written <= 0L) {
                runCatching { temp.delete() }
                return@withContext TrimResult.Failure(
                    "Nothing could be copied from that range. Try a different start point."
                )
            }

            onProgress(1f)

            val base = displayName
                .substringBeforeLast('.', displayName)
                .ifBlank { "VidVibe clip" }
                .take(48)
            val safeBase = base.replace(Regex("[^A-Za-z0-9 _-]"), "").trim()
                .ifBlank { "VidVibe clip" }
            val clipName = String.format(
                Locale.US,
                "VidVibe_%s_%d-%d.%s",
                safeBase,
                startMs / 1000L,
                endMs / 1000L,
                extension
            )

            val repository = VideoRepository(context)
            val uri = repository.saveToGallery(
                temp,
                clipName,
                repository.mimeTypeFor(clipName)
            )
            runCatching { temp.delete() }
            TrimResult.Success(uri, clipName, endMs - startMs)
        } catch (error: Throwable) {
            runCatching { muxer?.release() }
            runCatching { output?.delete() }
            TrimResult.Failure(
                error.message
                    ?.substringBefore('\n')
                    ?.takeIf { it.isNotBlank() }
                    ?: "The clip could not be saved."
            )
        } finally {
            runCatching { extractor.release() }
        }
    }

    private companion object {
        const val MIN_RANGE_MS = 500L
        const val BUFFER_BYTES = 2 * 1024 * 1024

        val MP4_VIDEO_MIMES = setOf(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            MediaFormat.MIMETYPE_VIDEO_MPEG4
        )
        val WEBM_VIDEO_MIMES = setOf(
            MediaFormat.MIMETYPE_VIDEO_VP8,
            MediaFormat.MIMETYPE_VIDEO_VP9,
            MediaFormat.MIMETYPE_VIDEO_AV1
        )
        val MP4_AUDIO_MIMES = setOf(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            MediaFormat.MIMETYPE_AUDIO_AMR_NB,
            MediaFormat.MIMETYPE_AUDIO_AMR_WB
        )
        val WEBM_AUDIO_MIMES = setOf(
            MediaFormat.MIMETYPE_AUDIO_OPUS,
            MediaFormat.MIMETYPE_AUDIO_VORBIS
        )
    }
}
