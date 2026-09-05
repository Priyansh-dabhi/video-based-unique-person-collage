package com.example.video_basedunique_personcollage

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.video_basedunique_personcollage.data.clustering.FaceClusterer
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.utils.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class FaceClustererTest {



    @Test
    fun testCosineSimilarity_identicalVectors_returnsOne() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(1f, 0f, 0f)
        val sim = MathUtils.cosineSimilarity(a, b)
        assertEquals(1.0f, sim, 0.0001f)
    }

    @Test
    fun testCosineSimilarity_orthogonalVectors_returnsZero() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(0f, 1f, 0f)
        val sim = MathUtils.cosineSimilarity(a, b)
        assertEquals(0.0f, sim, 0.0001f)
    }

    @Test
    fun testFaceClustering_groupsSimilarEmbeddings() {
        val dummyBitmap = Mockito.mock(Bitmap::class.java)
        val dummyRect = Mockito.mock(Rect::class.java)

        // Two faces of Person A (similar vectors)
        val faceA1 = FaceAnalysisResult(
            croppedBitmap = dummyBitmap,
            trackingId = 1,
            originalBoundingBox = dummyRect,
            smileProbability = null,
            leftEyeOpenProbability = null,
            rightEyeOpenProbability = null,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            timestampMs = 0L,
            embedding = floatArrayOf(0.9f, 0.1f, 0f)
        )
        val faceA2 = FaceAnalysisResult(
            croppedBitmap = dummyBitmap,
            trackingId = 1,
            originalBoundingBox = dummyRect,
            smileProbability = null,
            leftEyeOpenProbability = null,
            rightEyeOpenProbability = null,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            timestampMs = 333L,
            embedding = floatArrayOf(0.88f, 0.12f, 0f)
        )

        // One face of Person B (orthogonal vector)
        val faceB1 = FaceAnalysisResult(
            croppedBitmap = dummyBitmap,
            trackingId = 2,
            originalBoundingBox = dummyRect,
            smileProbability = null,
            leftEyeOpenProbability = null,
            rightEyeOpenProbability = null,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            timestampMs = 666L,
            embedding = floatArrayOf(0.0f, 0.1f, 0.95f)
        )

        val clusterer = FaceClusterer(similarityThreshold = 0.65f)
        val clusters = clusterer.cluster(listOf(faceA1, faceA2, faceB1))

        assertEquals(2, clusters.size)
        // Person A cluster should have 2 faces, Person B should have 1
        val clusterA = clusters.find { it.faceResults.contains(faceA1) }
        val clusterB = clusters.find { it.faceResults.contains(faceB1) }

        assertEquals(2, clusterA?.faceResults?.size)
        assertEquals(1, clusterB?.faceResults?.size)
    }

    @Test
    fun testAppearanceCounting_countsSegmentGaps() {
        val dummyBitmap = Mockito.mock(Bitmap::class.java)
        val dummyRect = Mockito.mock(Rect::class.java)

        // Continuous appearance 1: 0ms, 333ms, 666ms
        // Gap > 1200ms
        // Continuous appearance 2: 3000ms, 3333ms
        // Gap > 1200ms
        // Continuous appearance 3: 7000ms
        val timestamps = listOf(0L, 333L, 666L, 3000L, 3333L, 7000L)
        val faces = timestamps.map { t ->
            FaceAnalysisResult(
                croppedBitmap = dummyBitmap,
                trackingId = 1,
                originalBoundingBox = dummyRect,
                smileProbability = null,
                leftEyeOpenProbability = null,
                rightEyeOpenProbability = null,
                headEulerAngleX = 0f,
                headEulerAngleY = 0f,
                headEulerAngleZ = 0f,
                timestampMs = t,
                embedding = floatArrayOf(1f, 0f, 0f)
            )
        }

        val clusterer = FaceClusterer(similarityThreshold = 0.65f, maxGapForAppearanceMs = 1200L)
        val clusters = clusterer.cluster(faces)

        assertEquals(1, clusters.size)
        assertEquals(3, clusters[0].appearanceCount)
    }

    @Test
    fun testFaceClustering_sameFrameFaces_neverClusteredTogether() {
        val dummyBitmap = Mockito.mock(Bitmap::class.java)
        val dummyRect = Mockito.mock(Rect::class.java)

        // Two different people appearing at the exact same timestamp (0ms)
        // Even if embeddings are identical, they must NEVER be placed in the same cluster
        val face1 = FaceAnalysisResult(
            croppedBitmap = dummyBitmap,
            trackingId = 1,
            originalBoundingBox = dummyRect,
            smileProbability = null,
            leftEyeOpenProbability = null,
            rightEyeOpenProbability = null,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            timestampMs = 0L,
            embedding = floatArrayOf(1f, 0f, 0f)
        )
        val face2 = FaceAnalysisResult(
            croppedBitmap = dummyBitmap,
            trackingId = 2,
            originalBoundingBox = dummyRect,
            smileProbability = null,
            leftEyeOpenProbability = null,
            rightEyeOpenProbability = null,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            timestampMs = 0L,
            embedding = floatArrayOf(0.99f, 0.01f, 0f) // Highly similar
        )

        val clusterer = FaceClusterer(similarityThreshold = 0.5f)
        val clusters = clusterer.cluster(listOf(face1, face2))

        // Must produce 2 separate clusters despite high similarity because timestamp is identical
        assertEquals(2, clusters.size)
    }
}
