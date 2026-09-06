package com.example.video_basedunique_personcollage.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

/**
 * Lightweight helper to extract video metadata (duration, resolution) without
 * decoding frames, so the ViewModel can make adaptive sampling decisions.
 */
class VideoMetadataHelper {

    /**
     * Returns the duration of the video at [uri] in milliseconds,
     * or 0 if it cannot be determined.
     */
    fun getDurationMs(context: Context, uri: Uri): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val raw = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            raw?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w("VideoMetadataHelper", "Could not read duration for $uri: ${e.message}")
            0L
        } finally {
            runCatching { mmr.release() }
        }
    }
}
