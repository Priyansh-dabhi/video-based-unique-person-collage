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
     * Returns the single highest-scoring face in the cluster.
     * Falls back to the representative bitmap or first face if available.
     */
    fun selectBestShot(cluster: PersonCluster): FaceAnalysisResult? {
        if (cluster.faceResults.isEmpty()) return null

        return cluster.faceResults.maxByOrNull { calculatePhotogenicScore(it) }
    }

    /**
     * Calculates a composite photogenic quality score for a face.
     * Range typically 0..100+ (higher is better).
     */
    fun calculatePhotogenicScore(face: FaceAnalysisResult): Double {
        // 1. Sharpness component (0..100 capped)
        val sharpnessComponent = min(100.0, max(0.0, face.sharpnessScore)) * 0.40

        // 2. Smile component (0..1 -> 0..25 points)
        val smileProb = (face.smileProbability ?: 0f).coerceIn(0f, 1f)
        val smileComponent = smileProb * 25.0

        // 3. Eye openness component (both eyes open, no blinking) -> 0..30 points
        val leftEye = (face.leftEyeOpenProbability ?: 0.6f).coerceIn(0f, 1f)
        val rightEye = (face.rightEyeOpenProbability ?: 0.6f).coerceIn(0f, 1f)
        val bothEyesOpen = min(leftEye, rightEye)
        val eyeComponent = bothEyesOpen * 30.0

        // Blink / closed eyes heavy penalty (prevents blinks being chosen as hero shots)
        val blinkPenalty = if (bothEyesOpen < 0.45f) {
            (0.45f - bothEyesOpen) * 80.0
        } else {
            0.0
        }

        // 4. Frontal pose bonus / penalty
        // Total head rotation angle: pitch (X) + yaw (Y) + roll (Z)
        val totalRotation = abs(face.headEulerAngleX) + abs(face.headEulerAngleY) + abs(face.headEulerAngleZ)
        val posePenalty = if (totalRotation > 10f) {
            (totalRotation - 10f) * 2.0
        } else {
            0.0
        }

        // 5. Size component (rewards large, prominent faces, heavily penalizes small split-screen faces)
        val area = face.originalBoundingBox.width().toFloat() * face.originalBoundingBox.height().toFloat()
        // 150,000 pixels is roughly a 300x500 bounding box (typical large portrait face)
        val sizeComponent = min(1.0, area / 150000.0) * 50.0

        return sharpnessComponent + smileComponent + eyeComponent + sizeComponent - blinkPenalty - posePenalty
    }
}
