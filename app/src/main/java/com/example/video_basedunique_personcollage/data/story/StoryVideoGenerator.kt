package com.example.video_basedunique_personcollage.data.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

object StoryVideoGenerator {
    private const val TAG = "StoryVideoGenerator"

    suspend fun generateStoryVideo(
        context: Context,
        bitmaps: List<Bitmap>,
        config: StoryVideoConfig = StoryVideoConfig(),
        onProgress: (Float) -> Unit
    ): StoryVideoResult = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) {
            return@withContext StoryVideoResult.Error("No images provided for story.")
        }

        val cacheDir = File(context.cacheDir, "story_videos").apply { mkdirs() }
        val outputFile = File(cacheDir, "story_${System.currentTimeMillis()}.mp4")

        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var videoTrackIndex = -1
        var muxerStarted = false
        var inputSurface: android.view.Surface? = null

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, config.width, config.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val totalFramesPerImage = (config.frameDurationMs * config.fps / 1000).toInt()
            val totalTransitionFrames = (config.transitionDurationMs * config.fps / 1000).toInt()
            
            // Adjust frame count so transitions overlap
            // Total frames = (bitmaps.size * totalFramesPerImage) - ((bitmaps.size - 1) * totalTransitionFrames)
            val totalFrames = if (bitmaps.size == 1) {
                totalFramesPerImage
            } else {
                (bitmaps.size * totalFramesPerImage) - ((bitmaps.size - 1) * totalTransitionFrames)
            }
            
            val bufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10000L

            var currentFrame = 0
            var encodeDone = false
            var framesGenerated = 0

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }

            // Generation Loop
            while (isActive && (!encodeDone || framesGenerated < totalFrames)) {
                // 1. Generate Frame
                if (framesGenerated < totalFrames) {
                    val canvas = inputSurface.lockCanvas(null)
                    if (canvas != null) {
                        try {
                            renderFrame(canvas, framesGenerated, bitmaps, config, totalFramesPerImage, totalTransitionFrames, paint)
                        } finally {
                            inputSurface.unlockCanvasAndPost(canvas)
                        }
                        framesGenerated++
                        onProgress(framesGenerated.toFloat() / totalFrames)
                    }
                    
                    if (framesGenerated == totalFrames) {
                        encoder.signalEndOfInputStream()
                    }
                }

                // 2. Drain Encoder
                var encoderOutputAvailable = true
                while (encoderOutputAvailable) {
                    val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    when {
                        encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            encoderOutputAvailable = false
                        }
                        encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) {
                                throw RuntimeException("format changed twice")
                            }
                            val newFormat = encoder.outputFormat
                            videoTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        encoderStatus < 0 -> {
                            // Ignore other info/status
                        }
                        else -> {
                            val encodedData = encoder.getOutputBuffer(encoderStatus)
                                ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }

                            if (bufferInfo.size != 0) {
                                if (!muxerStarted) {
                                    throw RuntimeException("muxer hasn't started")
                                }
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                
                                // PTS calculation
                                bufferInfo.presentationTimeUs = computePresentationTime(currentFrame, config.fps)
                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                                currentFrame++
                            }

                            encoder.releaseOutputBuffer(encoderStatus, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encodeDone = true
                                encoderOutputAvailable = false
                            }
                        }
                    }
                }
            }

            if (!isActive) {
                return@withContext StoryVideoResult.Error("Generation cancelled by user")
            }

            val finalDurationMs = (totalFrames * 1000L) / config.fps
            StoryVideoResult.Success(android.net.Uri.fromFile(outputFile), finalDurationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating story video", e)
            outputFile.delete()
            StoryVideoResult.Error("Failed to encode video", e)
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
                inputSurface?.release()
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing resources", e)
            }
        }
    }

    private fun renderFrame(
        canvas: Canvas,
        frameIndex: Int,
        bitmaps: List<Bitmap>,
        config: StoryVideoConfig,
        framesPerImage: Int,
        transitionFrames: Int,
        paint: Paint
    ) {
        canvas.drawColor(android.graphics.Color.BLACK)

        // Find which image(s) we are currently displaying based on the frameIndex
        val framesPerCycle = framesPerImage - transitionFrames
        
        var currentIndex = frameIndex / framesPerCycle
        if (currentIndex >= bitmaps.size) currentIndex = bitmaps.size - 1
        
        val frameInCycle = frameIndex - (currentIndex * framesPerCycle)
        val isInTransition = frameInCycle >= framesPerCycle && currentIndex < bitmaps.size - 1

        val currentBitmap = bitmaps[currentIndex]
        
        // Draw current bitmap
        val progress1 = if (currentIndex == bitmaps.size - 1) {
            (frameInCycle.toFloat() / framesPerImage).coerceIn(0f, 1f)
        } else {
            (frameInCycle.toFloat() / framesPerImage).coerceIn(0f, 1f)
        }
        
        drawKenBurnsImage(canvas, currentBitmap, config, progress1, paint, alpha = 1f)

        // Draw next bitmap fading in if in transition
        if (isInTransition) {
            val nextBitmap = bitmaps[currentIndex + 1]
            val transitionProgress = (frameInCycle - framesPerCycle).toFloat() / transitionFrames
            val progress2 = (frameInCycle - framesPerCycle).toFloat() / framesPerImage // Next image just starting its Ken Burns
            
            paint.alpha = (transitionProgress * 255).toInt().coerceIn(0, 255)
            drawKenBurnsImage(canvas, nextBitmap, config, progress2, paint, alpha = transitionProgress)
            paint.alpha = 255 // reset
        }
    }

    private fun drawKenBurnsImage(
        canvas: Canvas,
        bitmap: Bitmap,
        config: StoryVideoConfig,
        progress: Float,
        paint: Paint,
        alpha: Float
    ) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val dstW = config.width.toFloat()
        val dstH = config.height.toFloat()

        val srcAspect = srcW / srcH
        val dstAspect = dstW / dstH

        val baseScale = if (srcAspect > dstAspect) {
            dstH / srcH
        } else {
            dstW / srcW
        }

        // Zoom from 1.05x to 1.15x for a subtle cinematic Ken Burns effect
        val currentZoom = 1.05f + (progress * 0.1f)
        val scale = baseScale * currentZoom

        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val dx = (dstW - scaledW) / 2f
        val dy = (dstH - scaledH) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        
        if (alpha < 1f) {
            paint.alpha = (alpha * 255).toInt()
        } else {
            paint.alpha = 255
        }
        
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun computePresentationTime(frameIndex: Int, fps: Int): Long {
        return 132 + frameIndex * 1000000L / fps
    }
}
