package com.example.video_basedunique_personcollage.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_basedunique_personcollage.data.model.FrameData
import com.example.video_basedunique_personcollage.domain.video.VideoFrameExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Context
import com.example.video_basedunique_personcollage.data.video.MediaRetrieverExtractor
import com.example.video_basedunique_personcollage.domain.ml.FaceDetector
import com.example.video_basedunique_personcollage.data.ml.MlKitFaceDetectorImpl
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult

class MainViewModel(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetector
) : ViewModel() {

    private val _extractedFaces = MutableStateFlow<List<FaceAnalysisResult>>(emptyList())
    val extractedFaces: StateFlow<List<FaceAnalysisResult>> = _extractedFaces.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun processVideo(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _extractedFaces.value = emptyList() // clear previous
            
            // Collect frames from the extractor
            videoFrameExtractor.extractFrames(uri, 333L).collect { frame ->
                val faces = faceDetector.detectFaces(frame)
                _extractedFaces.value = _extractedFaces.value + faces
            }
            
            _isProcessing.value = false
        }
    }
    
    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val extractor = MediaRetrieverExtractor(context.applicationContext)
                val detector = MlKitFaceDetectorImpl()
                MainViewModel(extractor, detector)
            }
        }
    }
}
