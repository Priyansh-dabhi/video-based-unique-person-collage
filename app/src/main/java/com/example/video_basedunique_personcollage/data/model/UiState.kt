package com.example.video_basedunique_personcollage.data.model

/**
 * Granular pipeline stage — used to drive the multi-step progress UI.
 */
enum class ProcessingStage {
    IDLE,
    EXTRACTING_FRAMES,
    DETECTING_FACES,
    EMBEDDING_FACES,
    CLUSTERING,
    DONE,
    ERROR
}

/**
 * Immutable snapshot of everything the UI needs to render.
 */
data class ProcessingProgress(
    val stage: ProcessingStage = ProcessingStage.IDLE,
    /** Frames decoded so far */
    val framesProcessed: Int = 0,
    /** Faces detected (before embedding) */
    val facesDetected: Int = 0,
    /** Faces with computed embeddings */
    val facesEmbedded: Int = 0,
    /** Message displayed beneath the progress indicator */
    val statusMessage: String = "",
    /** Error payload, populated only in ERROR stage */
    val errorMessage: String? = null
)
