package com.example.video_basedunique_personcollage.data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.video_basedunique_personcollage.data.model.FrameData
import com.example.video_basedunique_personcollage.domain.video.VideoFrameExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream

/**
 * A high-efficiency video frame extractor that uses Android's hardware-accelerated
 * MediaCodec + MediaExtractor pipeline to decode video frames.
 *
 * Unlike MediaMetadataRetriever (which does a random seek + decode per frame),
 * this class streams through the video sequentially — typically 5-10x faster
 * because the hardware decoder processes frames in their natural compressed order.
 *
 * Flow:
 *   MediaExtractor  ->  reads compressed H.264/HEVC packets from the file
 *   MediaCodec      ->  hardware-decodes packets into raw YUV_420_888 Images
 *   yuv420ToBitmap  ->  converts raw YUV to an ARGB Bitmap (stride-aware)
 *   emit()          ->  emits one frame per intervalMs window
 */
class MediaCodecVideoExtractor(private val context: Context) : VideoFrameExtractor {

    companion object {
        private const val TAG = "MediaCodecExtractor"

        // How long we wait for the decoder to hand back a buffer before looping.
        // 10ms is a good balance between responsiveness and avoiding a busy spin.
        private const val CODEC_TIMEOUT_US = 10_000L
    }

    override suspend fun extractFrames(uri: Uri, intervalMs: Long): Flow<FrameData> = flow {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            // 1. Open the video file
            extractor.setDataSource(context, uri, null)

            // 2. Find and select the video track
            val videoTrack = findVideoTrack(extractor)
            if (videoTrack == null) {
                Log.e(TAG, "No video track found in: $uri")
                return@flow
            }
            val (trackIndex, trackFormat) = videoTrack
            extractor.selectTrack(trackIndex)

            val videoWidth  = trackFormat.getInteger(MediaFormat.KEY_WIDTH)
            val videoHeight = trackFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val mimeType    = trackFormat.getString(MediaFormat.KEY_MIME) ?: return@flow

            Log.d(TAG, "Starting decode: ${videoWidth}x${videoHeight}, mime=$mimeType")

            // 3. Create and start the hardware decoder
            codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(trackFormat, null, null, 0 /* decoder mode */)
            codec.start()

            // 4. Run the decode loop and emit one frame per intervalMs
            decodeAndEmit(extractor, codec, intervalMs, videoWidth, videoHeight) { bmp, tsMs ->
                emit(FrameData(bmp, tsMs))
            }

        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec extraction failed: ${e.message}", e)
        } finally {
            // Release resources in reverse order; swallow individual failures
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }.flowOn(Dispatchers.IO)

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Scans all tracks and returns the index + MediaFormat of the first
     * video track found, or null if the file has no video stream.
     */
    private fun findVideoTrack(extractor: MediaExtractor): Pair<Int, MediaFormat>? {
        for (i in 0 until extractor.trackCount) {
            val fmt  = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) return i to fmt
        }
        return null
    }

    /**
     * Core decode loop.
     *
     * Pushes compressed packets into [codec] and pulls decoded YUV_420_888
     * Images back out. Frames are only converted and emitted when their
     * presentation timestamp reaches [nextSampleTargetMs], which advances by
     * [intervalMs] each time — giving us one sampled frame per interval.
     */
    private suspend fun decodeAndEmit(
        extractor:   MediaExtractor,
        codec:       MediaCodec,
        intervalMs:  Long,
        videoWidth:  Int,
        videoHeight: Int,
        onFrame:     suspend (Bitmap, Long) -> Unit
    ) {
        val info               = MediaCodec.BufferInfo()
        var inputEos           = false  // true once all packets sent to decoder
        var outputEos          = false  // true once decoder signals end-of-stream
        var nextSampleTargetMs = 0L     // presentation time of next frame to capture

        while (!outputEos) {

            // Feed one compressed packet into the decoder
            if (!inputEos) {
                inputEos = feedInputBuffer(extractor, codec)
            }

            // Retrieve one decoded frame from the decoder
            val outIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)

            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // Decoder still working on it — spin
                }

                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d(TAG, "Decoder output format changed: ${codec.outputFormat}")
                }

                outIndex >= 0 -> {
                    val presentationMs = info.presentationTimeUs / 1_000L

                    if (presentationMs >= nextSampleTargetMs) {
                        // getOutputImage() returns a stride-aware YUV_420_888 Image,
                        // normalised by the platform regardless of hardware colour format.
                        val image = codec.getOutputImage(outIndex)
                        if (image != null) {
                            val bitmap = yuv420ToBitmap(image, videoWidth, videoHeight)
                            image.close() // must close before releasing the buffer slot
                            if (bitmap != null) {
                                onFrame(bitmap, presentationMs)
                            }
                        }
                        nextSampleTargetMs = presentationMs + intervalMs
                    }

                    // Return the buffer slot to the decoder immediately
                    codec.releaseOutputBuffer(outIndex, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputEos = true
                    }
                }
            }
        }
    }

    /**
     * Dequeues one free input slot from [codec] and feeds it one compressed
     * packet from [extractor].
     * Returns true when we have signalled EOS (no more packets to feed).
     */
    private fun feedInputBuffer(extractor: MediaExtractor, codec: MediaCodec): Boolean {
        val inIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inIndex < 0) return false // no slot free yet

        val buf       = codec.getInputBuffer(inIndex) ?: return false
        val bytesRead = extractor.readSampleData(buf, 0)

        return if (bytesRead < 0) {
            // Extractor exhausted — signal end-of-stream to the decoder
            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        } else {
            codec.queueInputBuffer(inIndex, 0, bytesRead, extractor.sampleTime, 0)
            extractor.advance()
            false
        }
    }

    /**
     * Converts an [Image] in YUV_420_888 format into an ARGB [Bitmap].
     *
     * Why this is non-trivial:
     * Android's YUV_420_888 format has three planes (Y, U, V), each with its
     * own rowStride (bytes per row, which can be > width due to hardware padding)
     * and pixelStride (bytes between adjacent pixels — 1 for planar, 2 for
     * semi-planar NV12/NV21 layouts).
     *
     * When pixelStride == 2, U and V share the same backing memory (already
     * interleaved). Naively reading buffer.remaining() for both would double-count
     * those bytes, producing a corrupt and oversized array that crashes YuvImage.
     *
     * Solution: walk each row pixel-by-pixel using rowStride and pixelStride,
     * copy only valid pixels into a correctly-sized NV21 array, then use
     * YuvImage to compress NV21 -> JPEG -> Bitmap.
     */
    private fun yuv420ToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        return try {
            val crop = image.cropRect
            val actualWidth = if (crop != null && crop.width() > 0) crop.width() else width
            val actualHeight = if (crop != null && crop.height() > 0) crop.height() else height
            val cropLeft = if (crop != null && crop.width() > 0) crop.left else 0
            val cropTop = if (crop != null && crop.height() > 0) crop.top else 0

            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val yRowStride    = yPlane.rowStride
            val uvRowStride   = uPlane.rowStride   // U and V always share the same rowStride
            val uvPixelStride = uPlane.pixelStride // 1 for planar, 2 for semi-planar

            val yBuf = yPlane.buffer
            val uBuf = uPlane.buffer
            val vBuf = vPlane.buffer

            // NV21 layout: (width * height) Y bytes, then (width * height / 2) interleaved V/U
            val nv21    = ByteArray(actualWidth * actualHeight * 3 / 2)
            var nv21Pos = 0

            // Copy Y plane row by row, stripping any row-end padding bytes
            for (row in 0 until actualHeight) {
                yBuf.position((cropTop + row) * yRowStride + cropLeft)
                yBuf.get(nv21, nv21Pos, actualWidth)
                nv21Pos += actualWidth
            }

            // Copy chroma plane row by row, pixel by pixel (handles both stride=1 and stride=2)
            val chromaHeight = actualHeight / 2
            val chromaWidth  = actualWidth  / 2

            for (row in 0 until chromaHeight) {
                for (col in 0 until chromaWidth) {
                    val pixelOffset = (cropTop / 2 + row) * uvRowStride + (cropLeft / 2 + col) * uvPixelStride
                    nv21[nv21Pos++] = vBuf.get(pixelOffset) // NV21: V first, then U
                    nv21[nv21Pos++] = uBuf.get(pixelOffset)
                }
            }

            // NV21 -> JPEG -> Bitmap
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, actualWidth, actualHeight, null)
            val out      = ByteArrayOutputStream(actualWidth * actualHeight) // pre-size the stream
            yuvImage.compressToJpeg(Rect(0, 0, actualWidth, actualHeight), 85, out)
            val jpeg = out.toByteArray()

            android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

        } catch (e: Exception) {
            Log.e(TAG, "YUV->Bitmap conversion failed: ${e.message}", e)
            null
        }
    }
}
