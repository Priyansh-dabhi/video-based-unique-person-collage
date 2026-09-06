package com.example.video_basedunique_personcollage.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.video_basedunique_personcollage.data.model.FaceAnalysisResult
import com.example.video_basedunique_personcollage.data.model.ProcessingProgress
import com.example.video_basedunique_personcollage.data.model.ProcessingStage
import com.example.video_basedunique_personcollage.ui.components.*
import com.example.video_basedunique_personcollage.ui.theme.*

@Composable
fun ProcessingScreenView(
    progress: ProcessingProgress,
    recentFaces: List<FaceAnalysisResult>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Sweeping laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "processing_anim")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser"
    )

    // Pulsing reticle
    val reticleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticleAlpha"
    )

    // Spinner rotation
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Determine numerical percentage
    val percentage = when (progress.stage) {
        ProcessingStage.EXTRACTING_FRAMES -> (progress.framesProcessed * 1.5f).coerceIn(5f, 35f).toInt()
        ProcessingStage.DETECTING_FACES -> (35 + (progress.facesDetected * 1.2f)).coerceIn(35f, 70f).toInt()
        ProcessingStage.EMBEDDING_FACES -> (70 + (progress.facesEmbedded * 0.8f)).coerceIn(70f, 92f).toInt()
        ProcessingStage.CLUSTERING -> 95
        ProcessingStage.DONE -> 100
        else -> 10
    }

    val latestFaceBitmap: Bitmap? = recentFaces.lastOrNull()?.let {
        it.alignedBitmap ?: it.croppedBitmap
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        BrandHeader(
            title = "Processing Queue",
            trailingAction = {
                TextButton(onClick = onCancel) {
                    Text(text = "Cancel", color = StitchError, fontSize = 13.sp)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Cinematic Viewport Container ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(StitchSurfaceContainerLowest)
                    .border(1.dp, StitchOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background video frame / latest face crop or darkroom vignette
                if (latestFaceBitmap != null && !latestFaceBitmap.isRecycled) {
                    Image(
                        bitmap = latestFaceBitmap.asImageBitmap(),
                        contentDescription = "Live Neural Viewport",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                        alpha = 0.7f
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(StitchSurfaceContainerHigh, StitchSurfaceContainerLowest)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✦", color = StitchPrimary.copy(alpha = 0.2f), fontSize = 90.sp)
                    }
                }

                // Vignette dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    StitchSurfaceContainerLowest.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    StitchSurfaceContainerLowest.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // HUD Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StitchSurfaceContainerLowest.copy(alpha = 0.85f))
                            .border(1.dp, StitchSecondary.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchSecondary)
                        )
                        Text(
                            text = "LIVE NEURAL PASS",
                            color = StitchSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Frame ${progress.framesProcessed} · 60fps",
                        color = StitchOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StitchSurfaceContainerLowest.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Sweeping Laser Scanline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = (laserOffset - 130).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, StitchSecondary, Color.Transparent)
                            )
                        )
                        .shadow(10.dp, spotColor = StitchSecondary)
                )

                // Face Detection Reticle Target
                Box(
                    modifier = Modifier
                        .size(width = 130.dp, height = 115.dp)
                        .border(
                            1.dp,
                            StitchSecondary.copy(alpha = reticleAlpha * 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Reticle Corner Brackets
                    HudCorner(
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
                        isTop = true,
                        isStart = true,
                        size = 11.dp
                    )
                    HudCorner(
                        modifier = Modifier.align(Alignment.TopEnd).padding(3.dp),
                        isTop = true,
                        isStart = false,
                        size = 11.dp
                    )
                    HudCorner(
                        modifier = Modifier.align(Alignment.BottomStart).padding(3.dp),
                        isTop = false,
                        isStart = true,
                        size = 11.dp
                    )
                    HudCorner(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp),
                        isTop = false,
                        isStart = false,
                        size = 11.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.2.dp, StitchSecondary.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(StitchSecondary)
                            )
                        }

                        Text(
                            text = "ID: #01 · 99.4%",
                            color = StitchSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StitchSurfaceContainerLowest.copy(alpha = 0.92f))
                                .border(0.5.dp, StitchSecondary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Floating Live Stat Bar Inside Frame Bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat 1: Faces Detected
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StitchSurfaceContainerLowest.copy(alpha = 0.88f))
                            .border(1.dp, StitchOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StitchSecondary)
                            )
                            Text(
                                text = "Faces",
                                color = StitchOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "${progress.facesDetected}",
                            color = StitchSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Stat 2: Embedded
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StitchSurfaceContainerLowest.copy(alpha = 0.88f))
                            .border(1.dp, StitchOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Embedded",
                            color = StitchOnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${progress.facesEmbedded}",
                            color = StitchPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Real-Time Neural Processing Card ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(StitchSurfaceContainerLow)
                    .border(1.dp, StitchOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(StitchSecondary)
                        )
                        Text(
                            text = "Neural Analysis",
                            color = StitchOnSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$percentage%",
                        color = StitchSecondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Precision High-Tech Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(StitchSurfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0.04f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        StitchPrimaryContainer,
                                        StitchPrimary,
                                        StitchSecondary
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = progress.statusMessage.ifBlank { "Analyzing video frames with neural pipeline…" },
                    color = StitchOnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

                // ── 3-Step High-Tech Checklist ─────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChecklistRow(
                        title = "Keyframe extraction & ocular validation",
                        isCompleted = progress.stage in listOf(
                            ProcessingStage.DETECTING_FACES,
                            ProcessingStage.EMBEDDING_FACES,
                            ProcessingStage.CLUSTERING,
                            ProcessingStage.DONE
                        ),
                        isActive = progress.stage == ProcessingStage.EXTRACTING_FRAMES
                    )
                    ChecklistRow(
                        title = "Face detection & landmark alignment",
                        isCompleted = progress.stage in listOf(
                            ProcessingStage.EMBEDDING_FACES,
                            ProcessingStage.CLUSTERING,
                            ProcessingStage.DONE
                        ),
                        isActive = progress.stage == ProcessingStage.DETECTING_FACES
                    )
                    ChecklistRow(
                        title = "Clustering FaceNet-512 embeddings",
                        isCompleted = progress.stage == ProcessingStage.DONE,
                        isActive = progress.stage in listOf(
                            ProcessingStage.EMBEDDING_FACES,
                            ProcessingStage.CLUSTERING
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Hardware Efficiency Micro Pill Notice ──────────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StitchSurfaceContainerHighest.copy(alpha = 0.7f))
                    .border(0.5.dp, StitchOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚡",
                    color = StitchSecondary,
                    fontSize = 13.sp,
                    lineHeight = 13.sp
                )
                Text(
                    text = "GPU Acceleration Active · Optimized for Battery",
                    color = StitchOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(Modifier.height(16.dp))

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = StitchSurfaceContainerHigh,
                    contentColor = StitchOnSurface
                ),
                border = BorderStroke(1.dp, StitchOutlineVariant)
            ) {
                Text(
                    text = "Cancel Extraction & Discard",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = StitchOnSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(StitchSecondaryContainer.copy(alpha = 0.25f))
                    .border(1.dp, StitchSecondary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
                    val stroke = 2.dp.toPx()
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.52f)
                        lineTo(size.width * 0.42f, size.height * 0.80f)
                        lineTo(size.width * 0.88f, size.height * 0.22f)
                    }
                    drawPath(
                        path = path,
                        color = StitchSecondary,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }
        } else if (isActive) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, StitchPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StitchPrimary)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.dp, StitchOutlineVariant.copy(alpha = 0.6f), CircleShape)
            )
        }

        Text(
            text = title,
            color = if (isCompleted || isActive) StitchOnSurface else StitchOnSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
