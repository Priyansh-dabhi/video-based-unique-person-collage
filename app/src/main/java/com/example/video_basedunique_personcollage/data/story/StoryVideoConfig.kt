package com.example.video_basedunique_personcollage.data.story

data class StoryVideoConfig(
    val width: Int = 720,
    val height: Int = 1280, // 9:16 aspect ratio
    val fps: Int = 30,
    val videoBitrate: Int = 5_000_000, // 5 Mbps
    val frameDurationMs: Long = 1500L, // 1.5s per image
    val transitionDurationMs: Long = 400L // 400ms crossfade
)
