package com.vidvibe.app.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Merges a video-only stream and an audio-only stream into a single MP4 file
 * using the platform MediaExtractor/MediaMuxer. No FFmpeg binary is required.
 *
 * The current download path only hands H.264 video and AAC audio to this
 * helper. Unsupported codecs raise an IOException so the caller can fall back
 * to a progressive (already muxed) stream.
 */
object MediaMuxerMerge {

    private const val BUFFER_SIZE = 1 shl 20

    fun merge(videoFile: File, audioFile: File, outputFile: File): File {
        if (!videoFile.exists() || videoFile.length() <= 0L) {
            throw IOException("The downloaded video stream is missing.")
        }
        if (!audioFile.exists() || audioFile.length() <= 0L) {
            throw IOException("The downloaded audio stream is missing.")
        }

        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.parentFile?.mkdirs()

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)

            val videoTrack = findTrack(videoExtractor, "video/")
                ?: throw IOException("The downloaded video has no video track.")
            val audioTrack = findTrack(audioExtractor, "audio/")
                ?: throw IOException("The downloaded audio has no audio track.")

            videoExtractor.selectTrack(videoTrack)
            audioExtractor.selectTrack(audioTrack)

            val videoFormat = videoExtractor.getTrackFormat(videoTrack)
            val audioFormat = audioExtractor.getTrackFormat(audioTrack)

            requireSupportedCodec(videoFormat, "video")
            requireSupportedCodec(audioFormat, "audio")

            muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            rotationHint(videoFile)?.let { muxer.setOrientationHint(it) }

            val videoOutTrack = muxer.addTrack(videoFormat)
            val audioOutTrack = muxer.addTrack(audioFormat)
            muxer.start()
            muxerStarted = true

            copyTrack(videoExtractor, muxer, videoOutTrack, videoFormat)
            copyTrack(audioExtractor, muxer, audioOutTrack, audioFormat)

            muxer.stop()
            muxerStarted = false
            muxer.release()
            muxer = null

            if (!outputFile.exists() || outputFile.length() <= 0L) {
                throw IOException("The merged video could not be written.")
            }
            return outputFile
        } catch (error: Throwable) {
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching { outputFile.delete() }
            if (error is IOException) throw error
            throw IOException(
                "This video uses a codec that cannot be merged on this device.",
                error
            )
        } finally {
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }
    }

    private fun findTrack(extractor: MediaExtractor, mimePrefix: String): Int? {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(mimePrefix) == true) return index
        }
        return null
    }

    private fun requireSupportedCodec(format: MediaFormat, kind: String) {
        val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
        val supported = when (kind) {
            "video" -> mime == MediaFormat.MIMETYPE_VIDEO_AVC ||
                mime == MediaFormat.MIMETYPE_VIDEO_HEVC
            else -> mime == MediaFormat.MIMETYPE_AUDIO_AAC
        }
        if (!supported) {
            throw IOException("Unsupported $kind codec: $mime")
        }
    }

    private fun rotationHint(file: File): Int? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val rotation = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0
            rotation.takeIf { it != 0 }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun copyTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        outputTrack: Int,
        format: MediaFormat
    ) {
        val declaredSize = runCatching {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        }.getOrDefault(0)
        val bufferSize = maxOf(BUFFER_SIZE, declaredSize)
            .coerceAtMost(16 * 1024 * 1024)
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        val info = MediaCodec.BufferInfo()

        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break

            buffer.position(0)
            buffer.limit(size)

            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
            info.flags = if (
                extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
            ) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else {
                0
            }

            muxer.writeSampleData(outputTrack, buffer, info)
            extractor.advance()
        }
    }
}
