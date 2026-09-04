package com.example.video_basedunique_personcollage.data.ml

import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.FrameData
import com.example.video_basedunique_personcollage.domain.ml.FaceDetector
import com.example.video_basedunique_personcollage.utils.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class MlKitFaceDetectorImpl : FaceDetector {

    // Configure ML Kit
    // 1. PERFORMANCE_MODE_FAST: We don't need highly accurate contours, just the bounding box.
    // 2. CLASSIFICATION_MODE_ALL: We need to know if they are smiling / eyes open for Phase 4.
    // 3. LANDMARK_MODE_NONE: We don't need to know exactly where the nose is for now.
    // 4. enableTracking(): CRITICAL for assigning a stable ID to a person across frames.
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    override suspend fun detectFaces(frame: FrameData): List<FaceAnalysisResult> {
        val inputImage = InputImage.fromBitmap(frame.bitmap, 0)
        
        return try {
            val faces = detector.process(inputImage).await()
            
            // Map ML Kit Faces to our domain model
            faces.mapNotNull { face ->
                // Filter out tiny faces (noise/people in the far background)
                // If the face is less than 5% of the frame width, ignore it.
                val minFaceWidth = frame.bitmap.width * 0.05
                if (face.boundingBox.width() < minFaceWidth) {
                    return@mapNotNull null
                }

                // Generously crop the face from the original bitmap
                val croppedBitmap = BitmapUtils.cropGenerously(frame.bitmap, face.boundingBox)

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
                    timestampMs = frame.timestampMs
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
