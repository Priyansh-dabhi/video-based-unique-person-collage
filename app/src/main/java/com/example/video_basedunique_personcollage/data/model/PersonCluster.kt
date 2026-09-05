package com.example.video_basedunique_personcollage.data.model

import android.graphics.Bitmap

data class PersonCluster(
    var id: Int,
    val faceResults: MutableList<FaceAnalysisResult> = mutableListOf(),
    val embeddings: MutableList<FloatArray> = mutableListOf(),
    var centroid: FloatArray = FloatArray(0),
    var appearanceCount: Int = 1
) {
    val representativeBitmap: Bitmap?
        get() = faceResults.firstOrNull()?.croppedBitmap
}
