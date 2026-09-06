package com.example.video_basedunique_personcollage.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.graphics.Bitmap
import com.example.video_basedunique_personcollage.data.clustering.FaceClusterer
import com.example.video_basedunique_personcollage.data.collage.CollageGenerator
import com.example.video_basedunique_personcollage.data.ml.ArcFaceEmbedder
import com.example.video_basedunique_personcollage.data.ml.FaceNetEmbedder
import com.example.video_basedunique_personcollage.data.ml.MlKitFaceDetectorImpl
import com.example.video_basedunique_personcollage.data.model.CollageStyle
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.data.model.ProcessingProgress
import com.example.video_basedunique_personcollage.data.model.ProcessingStage
import com.example.video_basedunique_personcollage.data.video.MediaCodecVideoExtractor
import com.example.video_basedunique_personcollage.domain.ml.FaceDetector
import com.example.video_basedunique_personcollage.domain.ml.FaceEmbedder
import com.example.video_basedunique_personcollage.domain.video.VideoFrameExtractor
import com.example.video_basedunique_personcollage.utils.CollageExporter
import com.example.video_basedunique_personcollage.utils.VideoMetadataHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val faceClusterer: FaceClusterer = FaceClusterer(),
    private val videoMetadataHelper: VideoMetadataHelper
) : ViewModel() {

    // ─── Public state flows ────────────────────────────────────────────────────
    private val _progress = MutableStateFlow(ProcessingProgress())
    val progress: StateFlow<ProcessingProgress> = _progress.asStateFlow()

    private val _clusters = MutableStateFlow<List<PersonCluster>>(emptyList())
    val clusters: StateFlow<List<PersonCluster>> = _clusters.asStateFlow()

    /** Live list of all faces detected so far (used for real-time counter). */
    private val _extractedFaces = MutableStateFlow<List<FaceAnalysisResult>>(emptyList())
    val extractedFaces: StateFlow<List<FaceAnalysisResult>> = _extractedFaces.asStateFlow()

    private val _collageBitmap = MutableStateFlow<Bitmap?>(null)
    val collageBitmap: StateFlow<Bitmap?> = _collageBitmap.asStateFlow()

    private val _selectedCollageStyle = MutableStateFlow(CollageStyle.MODERN_GRID)
    val selectedCollageStyle: StateFlow<CollageStyle> = _selectedCollageStyle.asStateFlow()

    private val _isGeneratingCollage = MutableStateFlow(false)
    val isGeneratingCollage: StateFlow<Boolean> = _isGeneratingCollage.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    /** Clusters the user has manually hidden (id-based) */
    private val _hiddenClusterIds = MutableStateFlow<Set<Int>>(emptySet())
    val hiddenClusterIds: StateFlow<Set<Int>> = _hiddenClusterIds.asStateFlow()

    /** Convenience: visible clusters = all clusters minus hidden ones */
    val visibleClusters: StateFlow<List<PersonCluster>> get() = _clusters // Filtered in UI

    private var processingJob: Job? = null

    // ─── Convenience helpers (backward-compatible with screens) ───────────────
    val isProcessing: StateFlow<Boolean> get() = MutableStateFlow(false).also {
        // Forwarded via progress.stage — screens should use progress.stage
    }

    // ─── Core: Process video ───────────────────────────────────────────────────
    fun processVideo(context: Context, uri: Uri) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _clusters.value = emptyList()
            _extractedFaces.value = emptyList()
            _collageBitmap.value = null
            _exportMessage.value = null
            _hiddenClusterIds.value = emptySet()

            val allFaces = mutableListOf<FaceAnalysisResult>()

            try {
                // ── Adaptive frame sampling ──────────────────────────────────
                // Sample at 250ms (4 fps) for clips < 30s to catch brief appearances across multiple frames.
                // Sample at 350ms (~3 fps) for longer clips to balance speed and coverage.
                val durationMs = videoMetadataHelper.getDurationMs(context, uri)
                val intervalMs = if (durationMs in 1..30_000L) 250L else 350L
                android.util.Log.d("MainViewModel", "Video duration=${durationMs}ms, samplingInterval=${intervalMs}ms")

                _progress.value = ProcessingProgress(
                    stage = ProcessingStage.EXTRACTING_FRAMES,
                    statusMessage = "Extracting video frames…"
                )

                withContext(Dispatchers.Default) {
                    var frameCount = 0
                    var totalFacesDetected = 0
                    var totalFacesEmbedded = 0

                    videoFrameExtractor.extractFrames(uri, intervalMs).collect { frame ->
                        frameCount++

                        // Detect faces in this frame
                        _progress.value = _progress.value.copy(
                            stage = ProcessingStage.DETECTING_FACES,
                            framesProcessed = frameCount,
                            statusMessage = "Frame $frameCount — detecting faces…"
                        )

                        val detectedFaces = faceDetector.detectFaces(frame)
                        totalFacesDetected += detectedFaces.size

                        _progress.value = _progress.value.copy(
                            facesDetected = totalFacesDetected,
                            stage = ProcessingStage.EMBEDDING_FACES,
                            statusMessage = "Frame $frameCount — embedding ${detectedFaces.size} face(s)…"
                        )

                        // Generate embedding for each face
                        for (face in detectedFaces) {
                            try {
                                val bitmapForEmbedding = face.alignedBitmap ?: face.croppedBitmap
                                face.embedding = faceEmbedder.generateEmbedding(bitmapForEmbedding)
                                totalFacesEmbedded++
                            } catch (e: Throwable) {
                                android.util.Log.e("MainViewModel", "Embedding error: ${e.message}", e)
                            }
                        }

                        allFaces.addAll(detectedFaces)
                        _extractedFaces.value = allFaces.toList()

                        _progress.value = _progress.value.copy(
                            facesEmbedded = totalFacesEmbedded,
                            statusMessage = "Frame $frameCount • $totalFacesDetected faces • $totalFacesEmbedded embedded"
                        )

                        // Immediately recycle the heavy frame — we only need the small crops
                        if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
                    }

                    // ── Clustering ────────────────────────────────────────────
                    _progress.value = _progress.value.copy(
                        stage = ProcessingStage.CLUSTERING,
                        statusMessage = "Clustering ${allFaces.size} faces into unique people…"
                    )

                    // ── Diagnostic: log pairwise similarity distribution ─────
                    val embeddedFaces = allFaces.filter { it.embedding != null }
                    if (embeddedFaces.size in 2..200) {
                        val sims = mutableListOf<Float>()
                        for (i in embeddedFaces.indices) {
                            for (j in i + 1 until embeddedFaces.size) {
                                val embA = embeddedFaces[i].embedding ?: continue
                                val embB = embeddedFaces[j].embedding ?: continue
                                sims.add(com.example.video_basedunique_personcollage.utils.MathUtils.cosineSimilarity(embA, embB))
                            }
                        }
                        sims.sort()
                        val below40 = sims.count { it < 0.40f }
                        val between40_50 = sims.count { it in 0.40f..0.50f }
                        val between50_55 = sims.count { it in 0.50f..0.55f }
                        val between55_60 = sims.count { it in 0.55f..0.60f }
                        val above60 = sims.count { it >= 0.60f }
                        android.util.Log.d("SimilarityDiag",
                            "Pairwise sim distribution (${sims.size} pairs): " +
                            "<0.40=${below40} | 0.40-0.50=${between40_50} | " +
                            "0.50-0.55=${between50_55} | 0.55-0.60=${between55_60} | >=0.60=${above60}")
                        if (sims.isNotEmpty()) {
                            android.util.Log.d("SimilarityDiag",
                                "min=${sims.first()}, median=${sims[sims.size/2]}, max=${sims.last()}")
                        }
                    }

                    val clustered = faceClusterer.cluster(allFaces)
                    _clusters.value = clustered

                    val finalMsg = when {
                        clustered.isEmpty() && allFaces.isEmpty() ->
                            "No faces detected in the video."
                        clustered.isEmpty() ->
                            "Detected ${allFaces.size} face crops but couldn't form stable clusters."
                        else ->
                            "Found ${clustered.size} unique people across ${allFaces.size} face detections."
                    }

                    _progress.value = ProcessingProgress(
                        stage = ProcessingStage.DONE,
                        framesProcessed = frameCount,
                        facesDetected = totalFacesDetected,
                        facesEmbedded = totalFacesEmbedded,
                        statusMessage = finalMsg
                    )
                }
            } catch (e: CancellationException) {
                _progress.value = ProcessingProgress(
                    stage = ProcessingStage.IDLE,
                    statusMessage = "Processing cancelled."
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                _progress.value = ProcessingProgress(
                    stage = ProcessingStage.ERROR,
                    statusMessage = "Error: ${e.message ?: "Unknown error"}",
                    errorMessage = e.message
                )
            }
        }
    }

    /** Cancel any in-flight video processing. */
    fun cancelProcessing() {
        processingJob?.cancel()
    }

    /** Cancel processing and reset state back to Home/Import view. */
    fun resetToHome() {
        processingJob?.cancel()
        _progress.value = ProcessingProgress(stage = ProcessingStage.IDLE)
        _clusters.value = emptyList()
        _extractedFaces.value = emptyList()
        _collageBitmap.value = null
        _hiddenClusterIds.value = emptySet()
    }

    // ─── Cluster editing ──────────────────────────────────────────────────────

    /** Hide (soft-delete) a cluster from the visible list and collage. */
    fun hideCluster(clusterId: Int) {
        _hiddenClusterIds.value = _hiddenClusterIds.value + clusterId
    }

    /** Restore a previously hidden cluster. */
    fun restoreCluster(clusterId: Int) {
        _hiddenClusterIds.value = _hiddenClusterIds.value - clusterId
    }

    /**
     * Merge cluster [sourceId] into cluster [targetId].
     * The source cluster is removed; its faces are appended to the target.
     */
    fun mergeClusters(targetId: Int, sourceId: Int) {
        val current = _clusters.value.toMutableList()
        val target = current.find { it.id == targetId } ?: return
        val source = current.find { it.id == sourceId } ?: return
        target.faceResults.addAll(source.faceResults)
        target.embeddings.addAll(source.embeddings)
        target.appearanceCount += source.appearanceCount
        current.remove(source)
        _clusters.value = current
        // Also remove source from hidden list if present
        _hiddenClusterIds.value = _hiddenClusterIds.value - sourceId
    }

    // ─── Collage ──────────────────────────────────────────────────────────────

    fun generateCollage(style: CollageStyle = _selectedCollageStyle.value) {
        val hidden = _hiddenClusterIds.value
        val activeClusters = _clusters.value.filter { it.id !in hidden }
        if (activeClusters.isEmpty()) return

        viewModelScope.launch {
            _isGeneratingCollage.value = true
            _selectedCollageStyle.value = style
            val bitmap = withContext(Dispatchers.Default) {
                CollageGenerator.generateCollage(activeClusters, style)
            }
            _collageBitmap.value = bitmap
            _isGeneratingCollage.value = false
        }
    }

    fun saveCollageToGallery(context: Context) {
        val bitmap = _collageBitmap.value ?: return
        val result = CollageExporter.saveToGallery(context, bitmap)
        _exportMessage.value = if (result.isSuccess) {
            "✓ Saved to Gallery → Pictures/UniquePersonCollage"
        } else {
            "Failed to save: ${result.exceptionOrNull()?.message}"
        }
    }

    fun shareCollage(context: Context) {
        val bitmap = _collageBitmap.value ?: return
        CollageExporter.shareCollage(context, bitmap)
    }

    fun clearExportMessage() { _exportMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        faceEmbedder.close()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                MainViewModel(
                    videoFrameExtractor = MediaCodecVideoExtractor(appContext),
                    faceDetector = MlKitFaceDetectorImpl(),
                    faceEmbedder = FaceNetEmbedder(appContext),
                    videoMetadataHelper = VideoMetadataHelper()
                )
            }
        }
    }
}
