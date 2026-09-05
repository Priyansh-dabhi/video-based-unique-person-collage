package com.example.video_basedunique_personcollage.data.ml

import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.FrameData
import com.example.video_basedunique_personcollage.domain.ml.FaceDetector
import com.example.video_basedunique_personcollage.utils.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class MlKitFaceDetectorImpl : FaceDetector {

    // Configure ML Kit
    // 1. PERFORMANCE_MODE_ACCURATE: Higher confidence, precise bounding boxes, reduces false positives.
    // 2. CLASSIFICATION_MODE_ALL: Needed for smile / eye openness probabilities in Phase 4.
    // 3. LANDMARK_MODE_NONE: Keeps memory low and inference fast.
    // 4. enableTracking(): Assigns a stable ID to a person across consecutive frames.
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setMinFaceSize(0.05f) // 5% of image width — catches smaller/distant faces (default was 10%)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    override suspend fun detectFaces(frame: FrameData): List<FaceAnalysisResult> {
        val inputImage = InputImage.fromBitmap(frame.bitmap, 0)
        
        return try {
            val rawFaces = detector.process(inputImage).await()
            val faces = suppressOverlappingFaces(rawFaces)
            
            val allBoxes = faces.map { it.boundingBox }

            // Map ML Kit Faces to our domain model
            faces.mapNotNull { face ->
                // Filter 1: Ignore tiny faces (distant background people / false textures)
                val minFaceWidth = maxOf(40, (frame.bitmap.width * 0.04).toInt())
                if (face.boundingBox.width() < minFaceWidth || face.boundingBox.height() < minFaceWidth) {
                    return@mapNotNull null
                }

                // Filter 2: Ignore extreme head poses (side profiles > 55° or severe tilt > 35°)
                // Extreme angles produce corrupted embeddings with low match scores
                if (kotlin.math.abs(face.headEulerAngleY) > 55f || kotlin.math.abs(face.headEulerAngleZ) > 35f) {
                    return@mapNotNull null
                }

                // Square crop the face, preventing overlap into neighboring faces in the frame
                val otherBoxes = allBoxes.filter { it != face.boundingBox }
                val croppedBitmap = BitmapUtils.cropGenerously(
                    bitmap = frame.bitmap,
                    rect = face.boundingBox,
                    paddingPercent = 0.08f,
                    otherFaceBoxes = otherBoxes
                ) ?: return@mapNotNull null

                // Filter 3: Reject blurry faces (motion blur / video transitions)
                // Threshold of 24.0 eliminates visibly blurry/smeared faces
                val sharpness = BitmapUtils.calculateSharpness(croppedBitmap)
                if (sharpness < 24.0) {
                    return@mapNotNull null
                }

                FaceAnalysisResult(
                    croppedBitmap = croppedBitmap,
                    trackingId = face.trackingId,
                    originalBoundingBox = face.boundingBox,
                    smileProbability = face.smilingProbability,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ,
                    timestampMs = frame.timestampMs,
                    sharpnessScore = sharpness
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Suppresses duplicate/nested bounding boxes detected on the same face in a single frame.
     * Keeps the larger, more complete face bounding box and eliminates partial/nested detections
     * (e.g. nose close-ups or dual-scale detector artifacts).
     */
    private fun suppressOverlappingFaces(faces: List<Face>): List<Face> {
        if (faces.size <= 1) return faces

        // Sort descending by bounding box area (fuller face first)
        val sorted = faces.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
        val kept = mutableListOf<Face>()

        for (candidate in sorted) {
            val candBox = candidate.boundingBox
            val candArea = candBox.width() * candBox.height()
            if (candArea <= 0) continue

            var isDuplicate = false
            for (accepted in kept) {
                val accBox = accepted.boundingBox
                val accArea = accBox.width() * accBox.height()

                val interLeft = maxOf(candBox.left, accBox.left)
                val interTop = maxOf(candBox.top, accBox.top)
                val interRight = minOf(candBox.right, accBox.right)
                val interBottom = minOf(candBox.bottom, accBox.bottom)

                val interW = maxOf(0, interRight - interLeft)
                val interH = maxOf(0, interBottom - interTop)
                val interArea = interW * interH

                if (interArea > 0) {
                    val iou = interArea.toFloat() / (candArea + accArea - interArea)
                    val containment = interArea.toFloat() / minOf(candArea, accArea)

                    // If overlap is high (IoU > 0.35) or smaller box is mostly inside larger box (> 0.50)
                    if (iou > 0.35f || containment > 0.50f) {
                        isDuplicate = true
                        break
                    }
                }
            }

            if (!isDuplicate) {
                kept.add(candidate)
            }
        }

        return kept
    }
}
