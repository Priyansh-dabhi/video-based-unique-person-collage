package com.example.video_basedunique_personcollage.data.collage

import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Evaluates all face instances of a person cluster and selects their single most
 * photogenic, sharpest, and best-composed image ("hero shot").
 *
 * Scoring factors:
 * - Sharpness: Rewards high-definition, crisp focus.
 * - Smile Probability: Rewards genuine smiling expressions.
 * - Eye Openness: Ensures both eyes are wide open (no blinks).
 * - Frontal Pose: Penalizes side angles and head tilts.
 */
object BestShotSelector {

    /**
     * Returns the hero face for this cluster.
     * Respects user-selected candidate at index 0, or falls back to the highest-scoring face.
     */
    fun selectBestShot(cluster: PersonCluster): FaceAnalysisResult? {
        if (cluster.faceResults.isEmpty()) return null

        return cluster.faceResults.firstOrNull() ?: cluster.faceResults.maxByOrNull { calculatePhotogenicScore(it) }
    }

    /**
     * Calculates a composite photogenic quality score for a face.
     * Range typically 0..100+ (higher is better).
     */
    fun calculatePhotogenicScore(face: FaceAnalysisResult): Double {
        // 1. Sharpness component (0..100 capped) - heavily prioritized
        val sharpnessComponent = min(100.0, max(0.0, face.sharpnessScore)) * 0.80

        // Penalty for low sharpness / blur: heavily penalize soft or motion-blurred detections
        val blurPenalty = if (face.sharpnessScore < 25.0) {
            (25.0 - face.sharpnessScore) * 4.0
        } else {
            0.0
        }

        // 2. Solo-person priority: Strongly favor frames where this person is the ONLY person in frame.
        // Heavily penalize dual-person or group frames so that the system picks a single-person frame.
        val multiFacePenalty = if (face.totalFacesInFrame > 1) {
            50.0 + (face.totalFacesInFrame - 1) * 25.0
        } else {
            0.0
        }
        val soloBonus = if (face.totalFacesInFrame == 1) 30.0 else 0.0

        // 3. Smile component (0..1 -> 0..20 points)
        val smileProb = (face.smileProbability ?: 0f).coerceIn(0f, 1f)
        val smileComponent = smileProb * 20.0

        // 4. Eye openness component (both eyes open, no blinking) -> 0..25 points
        val leftEye = (face.leftEyeOpenProbability ?: 0.6f).coerceIn(0f, 1f)
        val rightEye = (face.rightEyeOpenProbability ?: 0.6f).coerceIn(0f, 1f)
        val bothEyesOpen = min(leftEye, rightEye)
        val eyeComponent = bothEyesOpen * 25.0

        // Blink / closed eyes heavy penalty (prevents blinks being chosen as hero shots)
        val blinkPenalty = if (bothEyesOpen < 0.45f) {
            (0.45f - bothEyesOpen) * 80.0
        } else {
            0.0
        }

        // 5. Frontal pose bonus / penalty
        // Total head rotation angle: pitch (X) + yaw (Y) + roll (Z)
        val totalRotation = abs(face.headEulerAngleX) + abs(face.headEulerAngleY) + abs(face.headEulerAngleZ)
        val posePenalty = if (totalRotation > 10f) {
            (totalRotation - 10f) * 2.0
        } else {
            0.0
        }

        // 6. Size component (rewards prominent portrait faces)
        val area = face.originalBoundingBox.width().toFloat() * face.originalBoundingBox.height().toFloat()
        val sizeComponent = min(1.0, area / 150000.0) * 25.0

        return sharpnessComponent + soloBonus + smileComponent + eyeComponent + sizeComponent - blinkPenalty - posePenalty - blurPenalty - multiFacePenalty
    }
}
