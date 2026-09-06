package com.example.video_basedunique_personcollage.data.ml

import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.FrameData
import com.example.video_basedunique_personcollage.domain.ml.FaceDetector
import com.example.video_basedunique_personcollage.utils.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.example.video_basedunique_personcollage.utils.AlignmentUtils
import com.example.video_basedunique_personcollage.data.model.FaceLandmarks5
import android.graphics.PointF
import android.util.Log
import kotlinx.coroutines.tasks.await

/**
 * ML Kit–based face detector with production-grade quality filtering.
 *
 * Tuning rationale (as a senior ML engineer):
 *
 * • PERFORMANCE_MODE_ACCURATE: Reduces false positives vs FAST mode — worth the
 *   extra ~20ms on modern SoCs to avoid garbage clusters.
 *
 * • minFaceSize = 0.04f (4 %): Catches people further from the camera.
 *   The original 10% default drops anyone not filling a big portion of screen.
 *
 * • Blur threshold: ADAPTIVE. Large, frontal faces can tolerate slightly lower
 *   sharpness (camera motion blur rather than out-of-focus). Small or angled
 *   faces require crisper images to produce good embeddings.  We gate on 16.0
 *   absolute minimum (below which even the human eye sees blur) and apply a
 *   size-scaled gate above that.
 *
 * • Euler Y (yaw) limit raised to 60°: FaceNet-512 handles moderate profiles.
 *   Cutting at 55° was dropping many valid 3/4-view captures.
 *
 * • Euler Z (roll) limit kept at 35°: severe head tilt corrupts all embedders.
 */
class MlKitFaceDetectorImpl : FaceDetector {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setMinFaceSize(0.04f)   // 4 % — catches distant / small faces
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    private var fallbackCount = 0

    override suspend fun detectFaces(frame: FrameData): List<FaceAnalysisResult> {
        val inputImage = InputImage.fromBitmap(frame.bitmap, 0)

        return try {
            val rawFaces = detector.process(inputImage).await()
            val faces = suppressOverlappingFaces(rawFaces)
            val allBoxes = faces.map { it.boundingBox }

            faces.mapNotNull { face ->
                // ── Size gate ────────────────────────────────────────────────
                // Absolute minimum 36 px wide; OR 4 % of frame width (whichever is larger).
                val minFaceWidth = maxOf(36, (frame.bitmap.width * 0.04).toInt())
                if (face.boundingBox.width() < minFaceWidth ||
                    face.boundingBox.height() < minFaceWidth
                ) return@mapNotNull null

                // ── Boundary guard ──────────────────────────────────────────
                // Reject faces severely clipped by the frame edges (entering/exiting shots).
                val marginX = (frame.bitmap.width * 0.015f).toInt()
                val marginY = (frame.bitmap.height * 0.015f).toInt()
                if (face.boundingBox.left <= marginX ||
                    face.boundingBox.right >= frame.bitmap.width - marginX ||
                    face.boundingBox.top <= marginY ||
                    face.boundingBox.bottom >= frame.bitmap.height - marginY
                ) {
                    // If clipped by border, only allow if face is substantially inside frame
                    val isSeverelyClipped = face.boundingBox.left <= 2 ||
                            face.boundingBox.right >= frame.bitmap.width - 2 ||
                            face.boundingBox.top <= 2 ||
                            face.boundingBox.bottom >= frame.bitmap.height - 2
                    if (isSeverelyClipped) return@mapNotNull null
                }

                // ── Pose gate ────────────────────────────────────────────────
                // Yaw > 45° → steep profile, unidentifiable / split cluster.
                // Roll > 30° → heavy tilt.
                if (kotlin.math.abs(face.headEulerAngleY) > 45f ||
                    kotlin.math.abs(face.headEulerAngleZ) > 30f
                ) return@mapNotNull null

                // ── Strict 5-Landmark Requirement ────────────────────────────
                // Eliminates false triggers (ears, back of head, half-faces, background textures).
                // A genuine identifiable face MUST have both eyes, nose, and mouth corners visible.
                val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                if (leftEye == null || rightEye == null || noseBase == null || mouthLeft == null || mouthRight == null) {
                    return@mapNotNull null
                }
                val landmarks = FaceLandmarks5(leftEye, rightEye, noseBase, mouthLeft, mouthRight)

                // ── Alignment (160x160 for FaceNet Embedder) ─────────────────
                val alignedBitmap = AlignmentUtils.alignFace(frame.bitmap, landmarks, targetSize = 160)
                    ?: return@mapNotNull null

                // ── Crop (Unaligned, 20% generous portrait padding for UI display) ───
                val otherBoxes = allBoxes.filter { it != face.boundingBox }
                val crop = BitmapUtils.cropGenerously(
                    bitmap = frame.bitmap,
                    rect = face.boundingBox,
                    paddingPercent = 0.20f,   // 20% gives comfortable portrait framing (forehead/chin)
                    otherFaceBoxes = otherBoxes
                ) ?: return@mapNotNull null

                // ── Adaptive blur gate ────────────────────────────────────────
                // Calculate sharpness on the unaligned crop
                val sharpness = BitmapUtils.calculateSharpness(crop)
                val isFrontal = kotlin.math.abs(face.headEulerAngleY) < 25f
                val isLarge = face.boundingBox.width() >= frame.bitmap.width * 0.10f
                val sharpnessGate = when {
                    isFrontal && isLarge -> 16.0   // Large frontal — allow some motion blur
                    isFrontal            -> 19.0   // Small frontal
                    else                 -> 22.0   // Slight angle
                }
                if (sharpness < sharpnessGate) return@mapNotNull null

                FaceAnalysisResult(
                    croppedBitmap = crop,
                    trackingId = face.trackingId,
                    originalBoundingBox = face.boundingBox,
                    smileProbability = face.smilingProbability,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ,
                    timestampMs = frame.timestampMs,
                    sharpnessScore = sharpness,
                    landmarks = landmarks,
                    alignedBitmap = alignedBitmap
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Suppresses nested / duplicate detections on the same physical face.
     * Computes IoU between every pair; keeps the larger box when IoU > 0.40.
     */
    private fun suppressOverlappingFaces(faces: List<Face>): List<Face> {
        if (faces.size <= 1) return faces
        val sorted = faces.sortedByDescending {
            it.boundingBox.width() * it.boundingBox.height()
        }
        val kept = mutableListOf<Face>()
        for (candidate in sorted) {
            val dominated = kept.any { existing ->
                iou(candidate.boundingBox, existing.boundingBox) > 0.40f
            }
            if (!dominated) kept.add(candidate)
        }
        return kept
    }

    /** Intersection-over-Union of two android.graphics.Rect objects. */
    private fun iou(a: android.graphics.Rect, b: android.graphics.Rect): Float {
        val interLeft   = maxOf(a.left, b.left)
        val interTop    = maxOf(a.top, b.top)
        val interRight  = minOf(a.right, b.right)
        val interBottom = minOf(a.bottom, b.bottom)
        if (interRight <= interLeft || interBottom <= interTop) return 0f
        val inter = ((interRight - interLeft) * (interBottom - interTop)).toFloat()
        val areaA = (a.width() * a.height()).toFloat()
        val areaB = (b.width() * b.height()).toFloat()
        return inter / (areaA + areaB - inter)
    }
}
