package com.example.video_basedunique_personcollage.data.model

import android.graphics.Bitmap
import android.graphics.Rect

data class FaceAnalysisResult(
    val croppedBitmap: Bitmap,
    val trackingId: Int?,
    val originalBoundingBox: Rect,
    val smileProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
    val timestampMs: Long
)
