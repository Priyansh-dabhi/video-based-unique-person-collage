package com.example.video_basedunique_personcollage.data.model

import android.graphics.Bitmap
import android.graphics.Rect

import android.graphics.PointF

data class FaceLandmarks5(
    val leftEye: PointF,
    val rightEye: PointF,
    val noseBase: PointF,
    val mouthLeft: PointF,
    val mouthRight: PointF
)

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
    val timestampMs: Long,
    val sharpnessScore: Double = 0.0,
    val landmarks: FaceLandmarks5? = null,
    val alignedBitmap: Bitmap? = null,
    var embedding: FloatArray? = null // Populated by FaceEmbedder
)
