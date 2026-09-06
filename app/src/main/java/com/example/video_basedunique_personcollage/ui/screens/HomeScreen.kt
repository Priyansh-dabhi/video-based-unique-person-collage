package com.example.video_basedunique_personcollage.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.video_basedunique_personcollage.data.model.ProcessingStage
import com.example.video_basedunique_personcollage.ui.MainViewModel
import com.example.video_basedunique_personcollage.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress by viewModel.progress.collectAsState()
    val clusters by viewModel.clusters.collectAsState()
    val extractedFaces by viewModel.extractedFaces.collectAsState()
    val hiddenIds by viewModel.hiddenClusterIds.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    val visibleClusters = remember(clusters, hiddenIds) {
        clusters.filter { it.id !in hiddenIds }
    }

    var showCollageDialog by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.processVideo(context, it) }
    }

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        val isProcessing = progress.stage in listOf(
            ProcessingStage.EXTRACTING_FRAMES,
            ProcessingStage.DETECTING_FACES,
            ProcessingStage.EMBEDDING_FACES,
            ProcessingStage.CLUSTERING
        )

        val isDoneWithResults = progress.stage == ProcessingStage.DONE && visibleClusters.isNotEmpty()

        AnimatedContent(
            targetState = when {
                isProcessing -> ScreenState.PROCESSING
                isDoneWithResults -> ScreenState.RESULTS
                else -> ScreenState.IMPORT
            },
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ScreenTransition"
        ) { state ->
            when (state) {
                ScreenState.IMPORT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (progress.stage == ProcessingStage.ERROR) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StitchErrorContainer.copy(alpha = 0.3f))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = progress.errorMessage ?: "Video processing failed. Please try another video.",
                                    color = StitchError,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (progress.stage == ProcessingStage.DONE && visibleClusters.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StitchSurfaceContainerHigh)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "No distinct people faces were identified. Try a video with closer portrait shots.",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        ImportScreenView(
                            onSelectVideo = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScreenState.PROCESSING -> {
                    ProcessingScreenView(
                        progress = progress,
                        recentFaces = extractedFaces,
                        onCancel = { viewModel.cancelProcessing() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ScreenState.RESULTS -> {
                    ResultsScreenView(
                        clusters = visibleClusters,
                        totalFacesDetected = progress.facesDetected,
                        onCreateCollage = { showCollageDialog = true },
                        onNewImport = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onHomeClick = {
                            viewModel.resetToHome()
                        },
                        onMergeClick = { targetId, sourceId ->
                            viewModel.mergeClusters(targetId, sourceId)
                        },
                        onToggleExclude = { clusterId ->
                            viewModel.hideCluster(clusterId)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showCollageDialog) {
            CollagePreviewDialog(
                viewModel = viewModel,
                onDismiss = { showCollageDialog = false }
            )
        }
    }
}

private enum class ScreenState {
    IMPORT,
    PROCESSING,
    RESULTS
}
