package com.example.video_basedunique_personcollage.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.video_basedunique_personcollage.data.collage.BestShotSelector
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import com.example.video_basedunique_personcollage.ui.components.BrandHeader
import com.example.video_basedunique_personcollage.ui.theme.*

@Composable
fun ResultsScreenView(
    clusters: List<PersonCluster>,
    totalFacesDetected: Int,
    onCreateCollage: () -> Unit,
    onNewImport: () -> Unit,
    onHomeClick: () -> Unit = {},
    onMergeClick: (targetId: Int, sourceId: Int) -> Unit,
    onToggleExclude: (clusterId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortByAppearances by remember { mutableStateOf(true) }
    var mergeTargetClusterId by remember { mutableStateOf<Int?>(null) }

    val sortedClusters = remember(clusters, sortByAppearances) {
        if (sortByAppearances) {
            clusters.sortedByDescending { it.appearanceCount }
        } else {
            clusters.sortedBy { it.id }
        }
    }

    val maxAppearances = clusters.maxOfOrNull { it.appearanceCount } ?: 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        BrandHeader(
            title = "Recognized Cast",
            trailingAction = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Home Screen Button
                    OutlinedButton(
                        onClick = onHomeClick,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StitchSurfaceContainerHigh,
                            contentColor = StitchOnSurface
                        ),
                        border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "⌂", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                            Text(text = "Home", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                        }
                    }

                    // New Video Button
                    FilledTonalButton(
                        onClick = onNewImport,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StitchPrimaryContainer.copy(alpha = 0.35f),
                            contentColor = StitchPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(text = "+ Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top Stats Cards ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stat 1: Unique People
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(StitchSurfaceContainer)
                            .border(1.dp, StitchSecondary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UNIQUE PEOPLE",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(StitchSecondaryContainer.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "👤", fontSize = 13.sp)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${clusters.size}",
                                    color = StitchSecondary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "clustered",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    // Stat 2: Faces Found
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(StitchSurfaceContainer)
                            .border(1.dp, StitchPrimary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FACES FOUND",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(StitchPrimaryContainer.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "✦", color = StitchPrimary, fontSize = 13.sp)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$totalFacesDetected",
                                    color = StitchPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "instances",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Primary Action CTA: Create Person Collage ──────────────────────
            item {
                Button(
                    onClick = onCreateCollage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = StitchPrimaryContainer),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        StitchPrimaryContainer,
                                        Color(0xFFB85EE6),
                                        StitchSecondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "✨", fontSize = 18.sp)
                            Text(
                                text = "Create Person Collage",
                                color = StitchOnPrimaryContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "→",
                                color = StitchOnPrimaryContainer,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Section Header with Sort Pill ─────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recognized Cast",
                            color = StitchOnSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(StitchSurfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${clusters.size}",
                                color = StitchOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Sort pill button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StitchSurfaceContainerHigh)
                            .clickable { sortByAppearances = !sortByAppearances }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⇅", color = StitchSecondary, fontSize = 12.sp)
                        Text(
                            text = if (sortByAppearances) "Most Appearances" else "Earliest Order",
                            color = StitchOnSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Person Cards ──────────────────────────────────────────────────
            itemsIndexed(sortedClusters, key = { _, c -> c.id }) { index, cluster ->
                val isLead = cluster.appearanceCount == maxAppearances && maxAppearances > 1
                val bestShot = remember(cluster) {
                    BestShotSelector.selectBestShot(cluster)?.let {
                        it.croppedBitmap ?: it.alignedBitmap
                    } ?: cluster.representativeBitmap
                }

                PersonCard(
                    cluster = cluster,
                    displayIndex = index + 1,
                    isLead = isLead,
                    bestShotBitmap = bestShot,
                    onMergeWith = {
                        mergeTargetClusterId = cluster.id
                    },
                    onExclude = {
                        onToggleExclude(cluster.id)
                    }
                )
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    // Merge Selection Dialog
    if (mergeTargetClusterId != null) {
        val targetCluster = clusters.find { it.id == mergeTargetClusterId }
        val otherClusters = clusters.filter { it.id != mergeTargetClusterId }

        AlertDialog(
            onDismissRequest = { mergeTargetClusterId = null },
            title = {
                Text(
                    text = "Merge into Person #${targetCluster?.id}",
                    color = StitchOnSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select another person to merge into this cluster:",
                        color = StitchOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                    otherClusters.forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StitchSurfaceContainerHigh)
                                .clickable {
                                    onMergeClick(mergeTargetClusterId!!, candidate.id)
                                    mergeTargetClusterId = null
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Person #${candidate.id}",
                                color = StitchOnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${candidate.appearanceCount} appearances",
                                color = StitchSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mergeTargetClusterId = null }) {
                    Text("Cancel", color = StitchOnSurfaceVariant)
                }
            },
            containerColor = StitchSurfaceContainer
        )
    }
}

@Composable
private fun PersonCard(
    cluster: PersonCluster,
    displayIndex: Int,
    isLead: Boolean,
    bestShotBitmap: Bitmap?,
    onMergeWith: () -> Unit,
    onExclude: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StitchSurfaceContainer)
            .border(1.dp, StitchOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: Avatar + Title + Gold Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Story Avatar Ring
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    StitchPrimaryContainer,
                                    StitchSecondary,
                                    StitchPrimary,
                                    StitchPrimaryContainer
                                )
                            )
                        )
                        .padding(2.5.dp)
                ) {
                    if (bestShotBitmap != null && !bestShotBitmap.isRecycled) {
                        Image(
                            bitmap = bestShotBitmap.asImageBitmap(),
                            contentDescription = "Person #$displayIndex Representative",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(StitchSurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👤", fontSize = 24.sp)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Person #$displayIndex",
                            color = StitchOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLead) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StitchPrimaryContainer.copy(alpha = 0.35f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Lead",
                                    color = StitchPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "${cluster.faceResults.size} detections · ${cluster.appearanceCount} appearances",
                        color = StitchOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // Gold Pill Badge: ×N
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StitchSecondaryContainer.copy(alpha = 0.2f))
                    .border(1.dp, StitchSecondary.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(text = "⚡", color = StitchSecondary, fontSize = 11.sp)
                Text(
                    text = "×${cluster.appearanceCount}",
                    color = StitchSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Horizontal Appearance Candidates Strip
        val horizontalScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cluster.faceResults.take(10).forEachIndexed { i, face ->
                val bmp = face.croppedBitmap ?: face.alignedBitmap
                if (bmp != null && !bmp.isRecycled) {
                    Box(
                        modifier = Modifier
                            .size(width = 66.dp, height = 78.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StitchSurfaceContainerHigh)
                            .border(
                                width = if (i == 0) 1.5.dp else 0.5.dp,
                                color = if (i == 0) StitchSecondary.copy(alpha = 0.8f) else StitchOutlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Candidate Shot $i",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle timestamp tag
                        val sec = (face.timestampMs / 1000).toInt()
                        val timeStr = "%02d:%02d".format(sec / 60, sec % 60)
                        Text(
                            text = timeStr,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onMergeWith,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = StitchSurfaceContainerHigh,
                    contentColor = StitchOnSurface
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(text = "Merge Group", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = onExclude,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = StitchErrorContainer.copy(alpha = 0.15f),
                    contentColor = StitchError
                ),
                border = BorderStroke(1.dp, StitchError.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(text = "Exclude", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = StitchError)
            }
        }
    }
}
