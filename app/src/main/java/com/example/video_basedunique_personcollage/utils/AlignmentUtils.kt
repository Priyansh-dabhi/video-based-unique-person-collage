package com.example.video_basedunique_personcollage.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.example.video_basedunique_personcollage.data.model.FaceLandmarks5

object AlignmentUtils {

    // ArcFace standard 112x112 canonical reference landmarks
    private val REF_POINTS_112 = arrayOf(
        PointF(38.2946f, 51.6963f), // Left Eye
        PointF(73.5318f, 51.5014f), // Right Eye
        PointF(56.0252f, 71.7366f), // Nose Base
        PointF(41.5493f, 92.3655f), // Mouth Left
        PointF(70.7299f, 92.2041f)  // Mouth Right
    )

    /**
     * Computes a similarity transform (rotation + uniform scale + translation) that maps
     * the detected landmarks to the ArcFace reference template using least squares.
     */
    fun estimateSimilarityTransform(source: FaceLandmarks5, targetSize: Int = 112): Matrix {
        val srcPoints = arrayOf(
            source.leftEye,
            source.rightEye,
            source.noseBase,
            source.mouthLeft,
            source.mouthRight
        )

        val dstPoints = if (targetSize == 112) {
            REF_POINTS_112
        } else {
            val scale = targetSize / 112f
            Array(5) { i ->
                PointF(REF_POINTS_112[i].x * scale, REF_POINTS_112[i].y * scale)
            }
        }

        // 1. Compute means
        var srcMeanX = 0f
        var srcMeanY = 0f
        var dstMeanX = 0f
        var dstMeanY = 0f

        for (i in 0..4) {
            srcMeanX += srcPoints[i].x
            srcMeanY += srcPoints[i].y
            dstMeanX += dstPoints[i].x
            dstMeanY += dstPoints[i].y
        }
        srcMeanX /= 5f
        srcMeanY /= 5f
        dstMeanX /= 5f
        dstMeanY /= 5f

        // 2. Subtract means
        var numA = 0f
        var numB = 0f
        var den = 0f

        for (i in 0..4) {
            val srcX = srcPoints[i].x - srcMeanX
            val srcY = srcPoints[i].y - srcMeanY
            val dstX = dstPoints[i].x - dstMeanX
            val dstY = dstPoints[i].y - dstMeanY

            numA += srcX * dstX + srcY * dstY
            numB += srcX * dstY - srcY * dstX
            den += srcX * srcX + srcY * srcY
        }

        val a = if (den != 0f) numA / den else 1f
        val b = if (den != 0f) numB / den else 0f

        val tx = dstMeanX - (a * srcMeanX - b * srcMeanY)
        val ty = dstMeanY - (b * srcMeanX + a * srcMeanY)

        val matrix = Matrix()
        matrix.setValues(floatArrayOf(
            a, -b, tx,
            b,  a, ty,
            0f, 0f, 1f
        ))
        return matrix
    }

    /**
     * Applies the similarity transform to the original bitmap, returning a 112x112 aligned face.
     */
    fun alignFace(bitmap: Bitmap, landmarks: FaceLandmarks5, targetSize: Int = 112): Bitmap? {
        if (bitmap.isRecycled) return null
        return try {
            val transform = estimateSimilarityTransform(landmarks, targetSize)
            val alignedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(alignedBitmap)
            val paint = Paint().apply {
                isFilterBitmap = true // smooth bilinear filtering
            }
            canvas.drawBitmap(bitmap, transform, paint)
            alignedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
