package com.example.video_basedunique_personcollage.data.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.video_basedunique_personcollage.data.model.FrameData
import com.example.video_basedunique_personcollage.domain.video.VideoFrameExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaRetrieverExtractor(private val context: Context) : VideoFrameExtractor {
    override suspend fun extractFrames(uri: Uri, intervalMs: Long): Flow<FrameData> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (durationMs > 0) {
                // MediaMetadataRetriever uses microseconds
                for (timeMs in 0 until durationMs step intervalMs) {
                    val bitmap = retriever.getFrameAtTime(
                        timeMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    
                    if (bitmap != null) {
                        emit(FrameData(bitmap, timeMs))
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }.flowOn(Dispatchers.IO)
}
