package com.example.video_basedunique_personcollage.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.data.model.ProcessingStage
import com.example.video_basedunique_personcollage.ui.MainViewModel
import com.example.video_basedunique_personcollage.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val progress by viewModel.progress.collectAsState()
    val clusters by viewModel.clusters.collectAsState()
    val hiddenIds by viewModel.hiddenClusterIds.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    val visibleClusters = remember(clusters, hiddenIds) {
        clusters.filter { it.id !in hiddenIds }
    }

    var showCollageDialog by remember { mutableStateOf(false) }
    var clusterToMergeInto by remember { mutableStateOf<Int?>(null) }

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
            .background(AppBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── App Header ─────────────────────────────────────────────────────
            item {
                AppHeader(
                    isProcessing = progress.stage in setOf(
                        ProcessingStage.EXTRACTING_FRAMES,
                        ProcessingStage.DETECTING_FACES,
                        ProcessingStage.EMBEDDING_FACES,
                        ProcessingStage.CLUSTERING
                    )
                ) {
                    if (progress.stage == ProcessingStage.EXTRACTING_FRAMES ||
                        progress.stage == ProcessingStage.DETECTING_FACES ||
                        progress.stage == ProcessingStage.EMBEDDING_FACES ||
                        progress.stage == ProcessingStage.CLUSTERING
                    ) {
                        viewModel.cancelProcessing()
                    } else {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                }
            }

            // ── Pipeline Progress ──────────────────────────────────────────────
            val isActive = progress.stage in setOf(
                ProcessingStage.EXTRACTING_FRAMES,
                ProcessingStage.DETECTING_FACES,
                ProcessingStage.EMBEDDING_FACES,
                ProcessingStage.CLUSTERING
            )
            if (isActive) {
                item {
                    PipelineProgressCard(progress.stage, progress.statusMessage)
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Error card ────────────────────────────────────────────────────
            if (progress.stage == ProcessingStage.ERROR) {
                item {
                    ErrorCard(progress.errorMessage ?: "Unknown error")
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Result summary + Create Collage ───────────────────────────────
            if (progress.stage == ProcessingStage.DONE && visibleClusters.isNotEmpty()) {
                item {
                    ResultSummaryCard(
                        uniquePeople = visibleClusters.size,
                        totalFaces = progress.facesDetected,
                        hiddenCount = hiddenIds.size
                    )
                    Spacer(Modifier.height(12.dp))
                    CreateCollageButton(
                        enabled = visibleClusters.isNotEmpty(),
                        onClick = { showCollageDialog = true }
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── No result empty state ─────────────────────────────────────────
            if (progress.stage == ProcessingStage.DONE && visibleClusters.isEmpty() && clusters.isNotEmpty()) {
                item {
                    EmptyResultCard("All clusters were hidden or no valid faces found.")
                }
            }

            // ── Cluster cards ─────────────────────────────────────────────────
            if (visibleClusters.isNotEmpty()) {
                item {
                    Text(
                        text = "Unique People",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(visibleClusters, key = { it.id }) { cluster ->
                    PersonClusterCard(
                        cluster = cluster,
                        allClusters = visibleClusters,
                        mergeTargetId = clusterToMergeInto,
                        onHide = { viewModel.hideCluster(cluster.id) },
                        onStartMerge = { clusterToMergeInto = cluster.id },
                        onMergeInto = { targetId ->
                            viewModel.mergeClusters(targetId, cluster.id)
                            clusterToMergeInto = null
                        },
                        onCancelMerge = { clusterToMergeInto = null }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Idle empty state ──────────────────────────────────────────────
            if (progress.stage == ProcessingStage.IDLE) {
                item {
                    IdleEmptyState()
                }
            }
        }

        // Floating action: restore hidden
        if (hiddenIds.isNotEmpty()) {
            TextButton(
                onClick = { hiddenIds.forEach { viewModel.restoreCluster(it) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(SurfaceElevated, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "↩ Restore ${hiddenIds.size} hidden person(s)",
                    color = PrimaryLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
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

// ─────────────────────────────────────────────────────────────────────────────
// App Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppHeader(isProcessing: Boolean, onButtonClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryDark.copy(alpha = 0.30f),
                        AppBackground
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo blob
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(PrimaryDefault, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👥", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FaceCollage AI",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Text(
                        text = "Find every unique person in your video",
                        fontSize = 13.sp,
                        color = OnSurfaceMuted
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProcessing) ErrorColor else PrimaryDefault,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (isProcessing) "⏹  Cancel Processing" else "🎬  Select Video to Analyze",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pipeline progress with 4-step visual tracker
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PipelineProgressCard(stage: ProcessingStage, message: String) {
    val steps = listOf(
        ProcessingStage.EXTRACTING_FRAMES to "Frames",
        ProcessingStage.DETECTING_FACES   to "Detect",
        ProcessingStage.EMBEDDING_FACES   to "Embed",
        ProcessingStage.CLUSTERING        to "Cluster"
    )
    val currentIdx = steps.indexOfFirst { it.first == stage }.coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { idx, (_, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val done = idx < currentIdx
                        val active = idx == currentIdx
                        val dotColor = when {
                            done   -> SuccessColor
                            active -> PrimaryDefault
                            else   -> OutlineVariant
                        }
                        val infinite = rememberInfiniteTransition(label = "pulse$idx")
                        val dotAlpha by infinite.animateFloat(
                            initialValue = 1f, targetValue = if (active) 0.5f else 1f,
                            animationSpec = infiniteRepeatable(
                                tween(700), RepeatMode.Reverse
                            ), label = "alpha$idx"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (active) 14.dp else 12.dp)
                                .alpha(if (active) dotAlpha else 1f)
                                .background(dotColor, CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            fontSize = 10.sp,
                            color = if (active || done) OnSurface else OnSurfaceMuted,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (idx < steps.lastIndex) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (idx < currentIdx) SuccessColor else OutlineVariant,
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PrimaryDefault,
                trackColor = OutlineVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 12.sp,
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result Summary Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ResultSummaryCard(uniquePeople: Int, totalFaces: Int, hiddenCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, PrimaryDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem("$uniquePeople", "Unique\nPeople", PrimaryDefault)
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = OutlineVariant
            )
            StatItem("$totalFaces", "Faces\nDetected", SecondaryDefault)
            if (hiddenCount > 0) {
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = OutlineVariant
                )
                StatItem("$hiddenCount", "Hidden", OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = OnSurfaceMuted, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Create Collage CTA Button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CreateCollageButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryDefault,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Text("✨  Create Person Collage", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("⚠️", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Something went wrong", fontWeight = FontWeight.Bold, color = ErrorColor)
                Spacer(Modifier.height(4.dp))
                Text(message, fontSize = 13.sp, color = OnSurfaceMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyResultCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, color = OnSurfaceMuted, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

@Composable
private fun IdleEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎥", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Select a video to discover\nevery unique person in it",
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Powered by FaceNet-512 AI",
                color = PrimaryDefault.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Person Cluster Card — the main interactive card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PersonClusterCard(
    cluster: PersonCluster,
    allClusters: List<PersonCluster>,
    mergeTargetId: Int?,
    onHide: () -> Unit,
    onStartMerge: () -> Unit,
    onMergeInto: (Int) -> Unit,
    onCancelMerge: () -> Unit
) {
    val isCurrentMergeSource = mergeTargetId == cluster.id
    val isMergeMode = mergeTargetId != null

    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrentMergeSource -> PrimaryDefault
            isMergeMode          -> OutlineVariant.copy(alpha = 0.5f)
            else                 -> OutlineVariant
        },
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (isMergeMode && !isCurrentMergeSource) {
                    Modifier.clickable { onMergeInto(cluster.id) }
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMergeSource) PrimaryDark.copy(alpha = 0.20f) else SurfaceCard
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ── Header row ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                val repBitmap = cluster.representativeBitmap
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .border(2.dp, PrimaryDefault, CircleShape)
                ) {
                    if (repBitmap != null && !repBitmap.isRecycled) {
                        Image(
                            bitmap = repBitmap.asImageBitmap(),
                            contentDescription = "Person ${cluster.id}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Person #${cluster.id}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = "${cluster.faceResults.size} detections • ${cluster.appearanceCount} appearance(s)",
                        fontSize = 12.sp,
                        color = OnSurfaceMuted
                    )
                }

                // Appearance badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SecondaryDefault.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, SecondaryDefault.copy(alpha = 0.50f))
                ) {
                    Text(
                        text = "×${cluster.appearanceCount}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SecondaryDefault
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Detected face strip ──────────────────────────────────────────
            Text(
                text = "All detections",
                fontSize = 11.sp,
                color = OnSurfaceMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cluster.faceResults.take(20)) { face ->
                    if (!face.croppedBitmap.isRecycled) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                        ) {
                            Image(
                                bitmap = face.croppedBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Timestamp chip
                            Text(
                                text = "%.1fs".format(face.timestampMs / 1000f),
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (cluster.faceResults.size > 20) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${cluster.faceResults.size - 20}",
                                color = OnSurfaceMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Action row ───────────────────────────────────────────────────
            if (!isMergeMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Merge button
                    OutlinedButton(
                        onClick = onStartMerge,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryDefault.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("⇄ Merge", fontSize = 13.sp, color = PrimaryDefault)
                    }
                    Spacer(Modifier.width(8.dp))
                    // Hide button
                    OutlinedButton(
                        onClick = onHide,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("✕ Remove", fontSize = 13.sp, color = ErrorColor)
                    }
                }
            } else if (isCurrentMergeSource) {
                // Merge mode: show tap-another instruction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryDark.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tap another card to merge it into this one",
                        fontSize = 12.sp,
                        color = PrimaryLight,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCancelMerge) {
                        Text("✕", color = OnSurfaceMuted, fontSize = 16.sp)
                    }
                }
            } else {
                // Another cluster is merge-source — highlight this as a target
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            PrimaryDefault.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, PrimaryDefault.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tap to merge into Person #${cluster.id}",
                        fontSize = 12.sp,
                        color = PrimaryDefault,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


