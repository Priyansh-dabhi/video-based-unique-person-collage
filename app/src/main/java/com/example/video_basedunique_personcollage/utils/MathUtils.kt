package com.example.video_basedunique_personcollage.utils

import kotlin.math.sqrt

object MathUtils {

    /**
     * Calculates the cosine similarity between two float vectors.
     * Returns a value between -1.0 and 1.0 (typically 0.0 to 1.0 for normalized face embeddings).
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        if (denominator == 0.0) return 0f

        return (dotProduct / denominator).toFloat()
    }

    /**
     * Computes the L2-normalized centroid (mean vector) for a collection of embeddings.
     */
    fun computeCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(0)
        val size = embeddings[0].size
        val sum = FloatArray(size)

        for (emb in embeddings) {
            for (i in 0 until size) {
                sum[i] += emb[i]
            }
        }

        val count = embeddings.size.toFloat()
        var sumSquares = 0f
        for (i in 0 until size) {
            sum[i] /= count
            sumSquares += sum[i] * sum[i]
        }

        val norm = sqrt(sumSquares.toDouble()).toFloat()
        if (norm > 0f) {
            for (i in 0 until size) {
                sum[i] /= norm
            }
        }

        return sum
    }
}
