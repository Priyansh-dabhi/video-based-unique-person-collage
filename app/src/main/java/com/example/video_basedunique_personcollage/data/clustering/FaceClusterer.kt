package com.example.video_basedunique_personcollage.data.clustering

import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.utils.MathUtils
import kotlin.math.abs

class FaceClusterer(
    private val similarityThreshold: Float = 0.65f,
    private val maxGapForAppearanceMs: Long = 1200L
) {

    /**
     * Groups a list of face analysis results into unique person clusters using greedy cosine clustering,
     * then computes the appearance count for each cluster.
     */
    fun cluster(faces: List<FaceAnalysisResult>): List<PersonCluster> {
        val clusters = mutableListOf<PersonCluster>()
        var nextId = 1

        for (face in faces) {
            val embedding = face.embedding ?: continue

            var bestCluster: PersonCluster? = null
            var highestSimilarity = -1f

            for (cluster in clusters) {
                val similarity = MathUtils.cosineSimilarity(embedding, cluster.centroid)
                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity
                    bestCluster = cluster
                }
            }

            if (bestCluster != null && highestSimilarity >= similarityThreshold) {
                bestCluster.faceResults.add(face)
                bestCluster.embeddings.add(embedding)
                bestCluster.centroid = MathUtils.computeCentroid(bestCluster.embeddings)
            } else {
                val newCluster = PersonCluster(
                    id = nextId++,
                    faceResults = mutableListOf(face),
                    embeddings = mutableListOf(embedding),
                    centroid = embedding.clone(),
                    appearanceCount = 1
                )
                clusters.add(newCluster)
            }
        }

        // Calculate appearance count and sort faces for best representation for each cluster
        for (cluster in clusters) {
            cluster.appearanceCount = calculateAppearances(cluster.faceResults, maxGapForAppearanceMs)
            // Sort faces so the one closest to frontal view (lowest absolute Euler Y/Z angles) is first
            cluster.faceResults.sortBy { face ->
                abs(face.headEulerAngleX) + abs(face.headEulerAngleY) + abs(face.headEulerAngleZ)
            }
        }

        return clusters
    }

    /**
     * Computes the number of distinct continuous appearances based on timestamp gaps.
     */
    private fun calculateAppearances(faces: List<FaceAnalysisResult>, maxGapMs: Long): Int {
        if (faces.isEmpty()) return 0
        val sortedTimestamps = faces.map { it.timestampMs }.sorted().distinct()
        if (sortedTimestamps.isEmpty()) return 0

        var appearances = 1
        for (i in 1 until sortedTimestamps.size) {
            val gap = sortedTimestamps[i] - sortedTimestamps[i - 1]
            if (gap > maxGapMs) {
                appearances++
            }
        }
        return appearances
    }
}
