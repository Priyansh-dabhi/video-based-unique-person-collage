package com.example.video_basedunique_personcollage.utils

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {
    /**
     * Crops a bitmap using a bounding box, adding a padding percentage to make the crop more generous.
     * Safe against out-of-bounds errors.
     */
    fun cropGenerously(
        bitmap: Bitmap,
        rect: Rect,
        paddingPercent: Float = 0.08f,
        otherFaceBoxes: List<Rect> = emptyList()
    ): Bitmap? {
        if (bitmap.isRecycled) return null

        val width = rect.width()
        val height = rect.height()

        if (width <= 0 || height <= 0) {
            return null
        }

        // Add modest proportional padding (8%) around the face without inflating width
        val padX = (width * paddingPercent).toInt()
        val padY = (height * paddingPercent).toInt()

        var left = rect.left - padX
        var top = rect.top - padY
        var right = rect.right + padX
        var bottom = rect.bottom + padY

        // Prevent encroaching on neighboring faces in multi-person shots
        for (other in otherFaceBoxes) {
            if (other.width() <= 0 || other.height() <= 0) continue

            // If the other face is to the right
            if (other.centerX() > rect.centerX() && other.left < right) {
                val boundary = (rect.right + other.left) / 2
                right = min(right, max(rect.right, boundary))
            }
            // If the other face is to the left
            if (other.centerX() < rect.centerX() && other.right > left) {
                val boundary = (other.right + rect.left) / 2
                left = max(left, min(rect.left, boundary))
            }
            // If the other face is below
            if (other.centerY() > rect.centerY() && other.top < bottom) {
                val boundary = (rect.bottom + other.top) / 2
                bottom = min(bottom, max(rect.bottom, boundary))
            }
            // If the other face is above
            if (other.centerY() < rect.centerY() && other.bottom > top) {
                val boundary = (other.bottom + rect.top) / 2
                top = max(top, min(rect.top, boundary))
            }
        }

        // Clamp to image bounds
        if (left < 0) {
            right = min(bitmap.width, right - left)
            left = 0
        }
        if (top < 0) {
            bottom = min(bitmap.height, bottom - top)
            top = 0
        }
        if (right > bitmap.width) {
            left = max(0, left - (right - bitmap.width))
            right = bitmap.width
        }
        if (bottom > bitmap.height) {
            top = max(0, top - (bottom - bitmap.height))
            bottom = bitmap.height
        }

        val cropWidth = right - left
        val cropHeight = bottom - top

        if (cropWidth <= 0 || cropHeight <= 0) {
            return null
        }

        return try {
            // Allocate an independent bitmap and draw into it with Canvas.
            // This guarantees:
            // 1. It NEVER shares native memory (SkPixelRef) with the original frame bitmap.
            // 2. When the heavy frame bitmap is recycled to save memory, this cropped face remains 100% intact.
            // 3. Only the small face region is kept in memory (~50KB instead of 8MB).
            val cropped = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(cropped)
            val srcRect = Rect(left, top, right, bottom)
            val dstRect = Rect(0, 0, cropWidth, cropHeight)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            cropped
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Resizes a face bitmap to the exact target dimensions (default 160x160 for FaceNet) required by the embedding model.
     * Uses bilinear filtering for smooth scaling.
     */
    fun resizeForEmbedding(bitmap: Bitmap, targetSize: Int = 160): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
    }

    /**
     * Computes the sharpness of a face bitmap using the variance of the 3x3 Laplacian operator.
     * A low score (< 35.0) indicates motion blur or out-of-focus blur.
     */
    fun calculateSharpness(bitmap: Bitmap): Double {
        if (bitmap.isRecycled) return 0.0
        val sampleSize = 64
        val scaled = if (bitmap.width != sampleSize || bitmap.height != sampleSize) {
            Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)
        } else {
            bitmap
        }
        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = 0.299 * r + 0.587 * g + 0.114 * b
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val center = gray[yOffset + x]
                val lap = gray[(y - 1) * width + x] +
                          gray[(y + 1) * width + x] +
                          gray[yOffset + (x - 1)] +
                          gray[yOffset + (x + 1)] -
                          4.0 * center
                sum += lap
                sumSq += lap * lap
                count++
            }
        }

        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSq / count) - (mean * mean)
    }
}
