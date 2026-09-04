package com.example.video_basedunique_personcollage.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.video_basedunique_personcollage.data.clustering.FaceClusterer
import com.example.video_basedunique_personcollage.data.ml.MlKitFaceDetectorImpl
import com.example.video_basedunique_personcollage.data.ml.MobileFaceNetEmbedder
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.data.video.MediaRetrieverExtractor
import com.example.video_basedunique_personcollage.domain.ml.FaceDetector
import com.example.video_basedunique_personcollage.domain.ml.FaceEmbedder
import com.example.video_basedunique_personcollage.domain.video.VideoFrameExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val faceClusterer: FaceClusterer = FaceClusterer()
) : ViewModel() {

    private val _clusters = MutableStateFlow<List<PersonCluster>>(emptyList())
    val clusters: StateFlow<List<PersonCluster>> = _clusters.asStateFlow()

    private val _extractedFaces = MutableStateFlow<List<FaceAnalysisResult>>(emptyList())
    val extractedFaces: StateFlow<List<FaceAnalysisResult>> = _extractedFaces.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun processVideo(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Analyzing video frames..."
            _clusters.value = emptyList()
            _extractedFaces.value = emptyList()

            val allFaces = mutableListOf<FaceAnalysisResult>()

            try {
                withContext(Dispatchers.Default) {
                    var frameCount = 0
                    // Step 1: Extract frames & detect faces & compute embeddings
                    videoFrameExtractor.extractFrames(uri, 333L).collect { frame ->
                        frameCount++
                        val detectedFaces = faceDetector.detectFaces(frame)

                        for (face in detectedFaces) {
                            try {
                                face.embedding = faceEmbedder.generateEmbedding(face.croppedBitmap)
                            } catch (e: Throwable) {
                                android.util.Log.e("MainViewModel", "Error generating embedding: ${e.message}", e)
                            }
                        }

                        allFaces.addAll(detectedFaces)
                        _extractedFaces.value = allFaces.toList()
                        _statusMessage.value = "Processed $frameCount frames (${allFaces.size} faces found)..."
                        
                        // Free the heavy original frame from memory to prevent OutOfMemoryError
                        frame.bitmap.recycle()
                    }

                    // Step 2: Cluster faces by identity & calculate appearances
                    _statusMessage.value = "Clustering faces and counting appearances..."
                    val clustered = faceClusterer.cluster(allFaces)
                    _clusters.value = clustered
                    if (clustered.isEmpty()) {
                        if (allFaces.isEmpty()) {
                            _statusMessage.value = "No faces detected in the video."
                        } else {
                            _statusMessage.value = "Detected ${allFaces.size} faces, but could not form clusters."
                        }
                    } else {
                        _statusMessage.value = "Done! Found ${clustered.size} unique people across ${allFaces.size} face detections."
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _statusMessage.value = "Error: ${e.message ?: "Out of Memory or unknown error"}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        faceEmbedder.close()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val extractor = MediaRetrieverExtractor(appContext)
                val detector = MlKitFaceDetectorImpl()
                val embedder = MobileFaceNetEmbedder(appContext)
                MainViewModel(extractor, detector, embedder)
            }
        }
    }
}
