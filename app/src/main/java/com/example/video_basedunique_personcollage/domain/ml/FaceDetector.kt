package com.example.video_basedunique_personcollage.domain.ml

import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.FrameData

interface FaceDetector {
    /**
     * Processes a single frame and returns a list of faces found in that frame.
     */
    suspend fun detectFaces(frame: FrameData): List<FaceAnalysisResult>
}
