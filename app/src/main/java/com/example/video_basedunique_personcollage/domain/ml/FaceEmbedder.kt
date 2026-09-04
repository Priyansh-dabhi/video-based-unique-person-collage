package com.example.video_basedunique_personcollage.domain.ml

import android.graphics.Bitmap

interface FaceEmbedder {
    /**
     * Extracts a 192-dimensional numerical embedding vector for a given cropped face bitmap.
     */
    fun generateEmbedding(faceBitmap: Bitmap): FloatArray

    /**
     * Releases any underlying model resources.
     */
    fun close()
}
