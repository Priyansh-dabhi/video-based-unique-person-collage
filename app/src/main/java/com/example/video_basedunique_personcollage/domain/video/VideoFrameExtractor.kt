package com.example.video_basedunique_personcollage.domain.video

import android.net.Uri
import com.example.video_basedunique_personcollage.data.model.FrameData
import kotlinx.coroutines.flow.Flow

interface VideoFrameExtractor {
    /**
     * Extracts frames from the given video URI.
     * @param uri The URI of the video.
     * @param intervalMs The interval between extracted frames in milliseconds (e.g., 500 for 2fps).
     * @return A Flow of FrameData containing the extracted Bitmaps and timestamps.
     */
    suspend fun extractFrames(uri: Uri, intervalMs: Long = 500L): Flow<FrameData>
}
