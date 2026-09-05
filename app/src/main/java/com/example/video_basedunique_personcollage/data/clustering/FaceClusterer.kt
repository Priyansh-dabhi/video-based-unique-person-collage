package com.example.video_basedunique_personcollage.data.clustering

import android.util.Log
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.utils.MathUtils
import kotlin.math.abs

class FaceClusterer(
    private val similarityThreshold: Float = 0.62f,
    private val maxGapForAppearanceMs: Long = 1200L,
    private val minFacesForValidCluster: Int = 3,
    private val strictMergeThreshold: Float = 0.62f,
    private val enableAgglomerativeMerge: Boolean = true
) {

    /**
     * Groups face analysis results into unique person clusters using:
     * 1. Tracklet-first grouping (leveraging ML Kit's trackingId).
     * 2. Temporal mutual exclusion (two faces in the same video frame can NEVER be the same person).
     * 3. Secondary agglomerative merge pass to unify multi-angle clusters of the same person.
     * 4. Fleeting noise/false-detection filtering.
     * 5. Sharpness-dominant avatar selection.
     */
    fun cluster(faces: List<FaceAnalysisResult>): List<PersonCluster> {
        val validFaces = faces.filter { it.embedding != null }
        Log.d("FaceClusterer", "Starting clustering: Total faces = ${faces.size}, with embeddings = ${validFaces.size}")
        if (validFaces.isEmpty()) return emptyList()

        // 1. Treat each face as its own tracklet
        val tracklets = validFaces.map { mutableListOf(it) }.toMutableList()

        // 2. Initial Clustering of Tracklets
        val clusters = mutableListOf<PersonCluster>()
        var nextId = 1

        for (tracklet in tracklets) {
            val trackletEmbeddings = tracklet.mapNotNull { it.embedding }
            if (trackletEmbeddings.isEmpty()) continue
            val trackletCentroid = MathUtils.computeCentroid(trackletEmbeddings)
            val trackletTimestamps = tracklet.map { it.timestampMs }.toSet()

            var bestCluster: PersonCluster? = null
            var highestSimilarity = -1f

            for (cluster in clusters) {
                // HARD CONSTRAINT: A person cannot be in two places at the exact same timestamp
                val clusterTimestamps = cluster.faceResults.map { it.timestampMs }.toSet()
                val hasTemporalConflict = clusterTimestamps.intersect(trackletTimestamps).isNotEmpty()
                if (hasTemporalConflict) {
                    continue
                }

                // Hybrid similarity: check both centroid and best-matching individual face
                val centroidSim = MathUtils.cosineSimilarity(trackletCentroid, cluster.centroid)
                val maxFaceSim = cluster.embeddings.maxOfOrNull { MathUtils.cosineSimilarity(trackletCentroid, it) } ?: 0f
                val similarity = maxOf(centroidSim, maxFaceSim * 0.96f)

                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity
                    bestCluster = cluster
                }
            }

            if (bestCluster != null && highestSimilarity >= similarityThreshold) {
                Log.d("FaceClusterer", "Merged tracklet (${tracklet.size} faces) into Cluster ${bestCluster.id} (sim: $highestSimilarity >= $similarityThreshold)")
                bestCluster.faceResults.addAll(tracklet)
                bestCluster.embeddings.addAll(trackletEmbeddings)
                bestCluster.centroid = MathUtils.computeCentroid(bestCluster.embeddings)
            } else {
                Log.d("FaceClusterer", "Created new Cluster $nextId for tracklet (${tracklet.size} faces, maxSim: $highestSimilarity vs thr: $similarityThreshold)")
                val newCluster = PersonCluster(
                    id = nextId++,
                    faceResults = tracklet.toMutableList(),
                    embeddings = trackletEmbeddings.toMutableList(),
                    centroid = trackletCentroid.clone(),
                    appearanceCount = 1
                )
                clusters.add(newCluster)
            }
        }

        // 3. Secondary Agglomerative Merge Pass to join clusters of the same person across cuts/angles
        if (enableAgglomerativeMerge) {
            var merged = true
            while (merged) {
                merged = false
                var bestI = -1
                var bestJ = -1
                var maxMergeSimilarity = strictMergeThreshold

                for (i in 0 until clusters.size) {
                    for (j in i + 1 until clusters.size) {
                        val clusterA = clusters[i]
                        val clusterB = clusters[j]

                        // Cannot merge if there is any timestamp collision
                        val timestampsA = clusterA.faceResults.map { it.timestampMs }.toSet()
                        val timestampsB = clusterB.faceResults.map { it.timestampMs }.toSet()
                        if (timestampsA.intersect(timestampsB).isNotEmpty()) {
                            continue
                        }

                        val centroidSim = MathUtils.cosineSimilarity(clusterA.centroid, clusterB.centroid)
                        val maxPairSim = clusterA.embeddings.maxOfOrNull { embA ->
                            clusterB.embeddings.maxOfOrNull { embB -> MathUtils.cosineSimilarity(embA, embB) } ?: 0f
                        } ?: 0f
                        val sim = maxOf(centroidSim, maxPairSim * 0.95f)

                        if (sim > maxMergeSimilarity) {
                            maxMergeSimilarity = sim
                            bestI = i
                            bestJ = j
                        }
                    }
                }

                if (bestI != -1 && bestJ != -1) {
                    val target = clusters[bestI]
                    val source = clusters[bestJ]
                    Log.d("FaceClusterer", "Agglomerative merge: Cluster ${source.id} into ${target.id} (sim: $maxMergeSimilarity >= $strictMergeThreshold)")
                    target.faceResults.addAll(source.faceResults)
                    target.embeddings.addAll(source.embeddings)
                    target.centroid = MathUtils.computeCentroid(target.embeddings)
                    clusters.removeAt(bestJ)
                    merged = true
                }
            }
        }

        // 4. Filter transient noise (single-frame blur/false positives)
        // If total detections across video is substantial (>= 6), require at least minFacesForValidCluster
        val filteredClusters = if (validFaces.size >= 6) {
            clusters.filter { it.faceResults.size >= minFacesForValidCluster }.ifEmpty { clusters }
        } else {
            clusters
        }

        // 5. Reassign IDs sequentially, compute appearances, and select sharpest, highest-quality avatar
        val finalClusters = filteredClusters.sortedByDescending { it.faceResults.size }
        finalClusters.forEachIndexed { index, cluster ->
            cluster.id = index + 1
            cluster.appearanceCount = calculateAppearances(cluster.faceResults, maxGapForAppearanceMs)
            // Sort faces by sharpness-dominant quality score to ensure the clearest avatar
            cluster.faceResults.sortWith(
                compareByDescending<FaceAnalysisResult> { face ->
                    val totalAngle = abs(face.headEulerAngleX) + abs(face.headEulerAngleY) + abs(face.headEulerAngleZ)
                    val anglePenalty = if (totalAngle > 15f) (totalAngle - 15f) * 1.5 else 0.0
                    face.sharpnessScore - anglePenalty
                }
            )
        }

        Log.d("FaceClusterer", "Clustering finished. Formed ${finalClusters.size} clusters from ${validFaces.size} faces.")
        return finalClusters
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
