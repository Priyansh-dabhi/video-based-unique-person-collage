package com.example.video_basedunique_personcollage.data.clustering

import android.util.Log
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.utils.MathUtils
import kotlin.math.abs
import kotlin.math.min

/**
 * Production face clusterer using **Chinese Whispers** with **Reciprocal K-Nearest Neighbor**
 * edge filtering, calibrated for FaceNet-512 embeddings.
 *
 * FaceNet-512 characteristics:
 * - Same person, varied pose: cosine similarity 0.50–0.85
 * - Different people:         cosine similarity 0.05–0.40
 * - FaceNet has GOOD inter-person separation but can over-split under pose variation
 *
 * Pipeline:
 * 1. Compute pairwise similarity matrix
 * 2. Find K-nearest neighbors for each face (excluding same-timestamp)
 * 3. Create weighted edges only between reciprocal KNN pairs
 * 4. Run Chinese Whispers label propagation
 * 5. Centroid-based merge pass to reunite over-split clusters
 * 6. Noise filtering for single-face clusters
 */
class FaceClusterer(
    /** Minimum cosine similarity for a reciprocal KNN edge. */
    private val minEdgeSimilarity: Float = 0.40f,
    /** Number of nearest neighbors for reciprocal check. */
    private val kNeighbors: Int = 12,
    /** Gap (ms) that breaks an appearance sequence. */
    private val maxGapForAppearanceMs: Long = 800L,
    /** Max Chinese Whispers iterations. */
    private val maxIterations: Int = 50,
    /** Centroid similarity threshold for post-CW merge pass. */
    private val centroidMergeThreshold: Float = 0.50f
) {

    companion object {
        private const val TAG = "FaceClusterer"
    }

    fun cluster(faces: List<FaceAnalysisResult>): List<PersonCluster> {
        val validFaces = faces.filter { it.embedding != null }
        Log.d(TAG, "Starting: total=${faces.size}, withEmbedding=${validFaces.size}")
        if (validFaces.isEmpty()) return emptyList()

        val n = validFaces.size
        if (n == 1) {
            return listOf(buildCluster(1, validFaces, validFaces.mapNotNull { it.embedding }))
        }

        // ─── Step 1: Pairwise similarity matrix ─────────────────────────────
        val sim = Array(n) { FloatArray(n) }
        for (i in 0 until n) {
            sim[i][i] = 1.0f
            for (j in i + 1 until n) {
                val s = MathUtils.cosineSimilarity(
                    validFaces[i].embedding!!, validFaces[j].embedding!!
                )
                sim[i][j] = s
                sim[j][i] = s
            }
        }

        // ─── Step 2: K-Nearest Neighbors (excluding same-timestamp) ─────────
        val effectiveK = min(kNeighbors, n - 1)
        val knnSets = Array(n) { i ->
            val tsI = validFaces[i].timestampMs
            (0 until n)
                .filter { j -> j != i && validFaces[j].timestampMs != tsI }
                .sortedByDescending { j -> sim[i][j] }
                .take(effectiveK)
                .toSet()
        }

        // ─── Step 3: Reciprocal KNN edges ───────────────────────────────────
        val adjacency = Array(n) { mutableListOf<Pair<Int, Float>>() }
        var edgeCount = 0

        for (i in 0 until n) {
            for (j in knnSets[i]) {
                if (j > i && i in knnSets[j]) {
                    val s = sim[i][j]
                    if (s >= minEdgeSimilarity) {
                        adjacency[i].add(Pair(j, s))
                        adjacency[j].add(Pair(i, s))
                        edgeCount++
                    }
                }
            }
        }

        val isolated = adjacency.count { it.isEmpty() }
        Log.d(TAG, "Reciprocal KNN graph: $n nodes, $edgeCount edges, $isolated isolated (K=$effectiveK)")

        // ─── Step 4: Chinese Whispers ───────────────────────────────────────
        val labels = IntArray(n) { it }
        var changed = true
        var iteration = 0

        while (changed && iteration < maxIterations) {
            changed = false
            iteration++
            for (nodeIdx in (0 until n).shuffled()) {
                val neighbors = adjacency[nodeIdx]
                if (neighbors.isEmpty()) continue
                val labelWeights = mutableMapOf<Int, Float>()
                for ((nIdx, w) in neighbors) {
                    val lbl = labels[nIdx]
                    labelWeights[lbl] = (labelWeights[lbl] ?: 0f) + w
                }
                val best = labelWeights.maxByOrNull { it.value }?.key ?: labels[nodeIdx]
                if (best != labels[nodeIdx]) {
                    labels[nodeIdx] = best
                    changed = true
                }
            }
        }

        Log.d(TAG, "CW converged after $iteration iterations")

        // ─── Step 5: Collect raw clusters ───────────────────────────────────
        val labelToIndices = mutableMapOf<Int, MutableList<Int>>()
        for (i in 0 until n) {
            labelToIndices.getOrPut(labels[i]) { mutableListOf() }.add(i)
        }
        Log.d(TAG, "Raw CW clusters: ${labelToIndices.size}")

        // ─── Step 6: Centroid-based merge pass ──────────────────────────────
        // FaceNet over-splits same person under pose variation.
        // Merge clusters whose centroids are similar AND have no temporal conflict.
        data class ClusterData(
            val indices: MutableList<Int>,
            var centroid: FloatArray
        )

        val clusterList = labelToIndices.values.map { idxList ->
            val embs = idxList.mapNotNull { validFaces[it].embedding }
            ClusterData(idxList.toMutableList(), MathUtils.computeCentroid(embs))
        }.toMutableList()

        var merged = true
        while (merged && clusterList.size > 1) {
            merged = false
            var bestI = -1
            var bestJ = -1
            var bestSim = 0f

            for (i in clusterList.indices) {
                for (j in i + 1 until clusterList.size) {
                    val ca = clusterList[i]
                    val cb = clusterList[j]

                    // Temporal conflict: can't merge if they share timestamps
                    val tsA = ca.indices.map { validFaces[it].timestampMs }.toHashSet()
                    if (cb.indices.any { validFaces[it].timestampMs in tsA }) continue

                    val cSim = MathUtils.cosineSimilarity(ca.centroid, cb.centroid)

                    // Also verify with cross-member average similarity
                    // (prevents centroid drift from causing wrong merges)
                    if (ca.indices.size >= 3 && cb.indices.size >= 3) {
                        var crossSum = 0f
                        var crossCount = 0
                        for (ai in ca.indices) {
                            for (bi in cb.indices) {
                                crossSum += sim[ai][bi]
                                crossCount++
                            }
                        }
                        val avgCross = if (crossCount > 0) crossSum / crossCount else 0f
                        // Both centroid sim AND average cross-member sim must pass
                        if (avgCross < centroidMergeThreshold - 0.05f) continue
                    }

                    if (cSim >= centroidMergeThreshold && cSim > bestSim) {
                        bestSim = cSim
                        bestI = i
                        bestJ = j
                    }
                }
            }

            if (bestI >= 0 && bestJ >= 0) {
                val target = clusterList[bestI]
                val source = clusterList[bestJ]
                Log.d(TAG, "Centroid merge: ${source.indices.size} faces → cluster with ${target.indices.size} (sim=${"%.3f".format(bestSim)})")
                target.indices.addAll(source.indices)
                val allEmbs = target.indices.mapNotNull { validFaces[it].embedding }
                target.centroid = MathUtils.computeCentroid(allEmbs)
                clusterList.removeAt(bestJ)
                merged = true
            }
        }

        Log.d(TAG, "After centroid merge: ${clusterList.size} clusters")

        // ─── Step 7: Keep all clusters (user can manually remove via Remove button) ──
        // No noise filtering — it was discarding the 5th person as a small cluster.
        val filtered = clusterList

        Log.d(TAG, "Final clusters: ${filtered.size}")

        // ─── Step 8: Build final PersonCluster objects ──────────────────────
        val result = filtered
            .sortedByDescending { it.indices.size }
            .mapIndexed { index, cd ->
                val faceList = cd.indices.map { validFaces[it] }
                val embList = cd.indices.mapNotNull { validFaces[it].embedding }
                buildCluster(index + 1, faceList, embList)
            }

        Log.d(TAG, "Final: ${result.size} unique people from ${validFaces.size} faces")
        return result
    }

    private fun buildCluster(id: Int, faces: List<FaceAnalysisResult>, embs: List<FloatArray>): PersonCluster {
        val cluster = PersonCluster(
            id = id,
            faceResults = faces.toMutableList(),
            embeddings = embs.toMutableList(),
            centroid = MathUtils.computeCentroid(embs),
            appearanceCount = calculateAppearances(faces, maxGapForAppearanceMs)
        )
        cluster.faceResults.sortWith(compareByDescending { face ->
            val totalAngle = abs(face.headEulerAngleX) + abs(face.headEulerAngleY) + abs(face.headEulerAngleZ)
            val anglePenalty = if (totalAngle > 15f) (totalAngle - 15f) * 1.5 else 0.0
            face.sharpnessScore - anglePenalty
        })
        return cluster
    }

    private fun calculateAppearances(faces: List<FaceAnalysisResult>, maxGapMs: Long): Int {
        if (faces.isEmpty()) return 0
        val sorted = faces.map { it.timestampMs }.sorted().distinct()
        var count = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] > maxGapMs) count++
        }
        return count
    }
}
