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
    fun cropGenerously(bitmap: Bitmap, rect: Rect, paddingPercent: Float = 0.4f): Bitmap {
        val width = rect.width()
        val height = rect.height()

        val paddingX = (width * paddingPercent).toInt()
        val paddingY = (height * paddingPercent).toInt()

        val left = max(0, rect.left - paddingX)
        val top = max(0, rect.top - paddingY)
        val right = min(bitmap.width, rect.right + paddingX)
        val bottom = min(bitmap.height, rect.bottom + paddingY)

        val cropWidth = right - left
        val cropHeight = bottom - top

        // If the bounding box is invalid (e.g. outside the image), just return the original
        if (cropWidth <= 0 || cropHeight <= 0) {
            return bitmap
        }

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }
}
