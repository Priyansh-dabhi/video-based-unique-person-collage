package com.example.video_basedunique_personcollage.data.story

import android.net.Uri

sealed class StoryVideoResult {
    data class Success(val videoUri: Uri, val durationMs: Long) : StoryVideoResult()
    data class Error(val message: String, val exception: Exception? = null) : StoryVideoResult()
}
