package com.example.video_basedunique_personcollage.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.video_basedunique_personcollage.data.model.CollageStyle
import com.example.video_basedunique_personcollage.ui.MainViewModel
import com.example.video_basedunique_personcollage.ui.theme.*

@Composable
fun CollagePreviewDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val collageBitmap by viewModel.collageBitmap.collectAsState()
    val selectedStyle by viewModel.selectedCollageStyle.collectAsState()
    val isGenerating by viewModel.isGeneratingCollage.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    val clusters by viewModel.clusters.collectAsState()
    val hiddenIds by viewModel.hiddenClusterIds.collectAsState()

    val visibleClusters = remember(clusters, hiddenIds) {
        clusters.filter { it.id !in hiddenIds }
    }
    val totalAppearances = remember(visibleClusters) {
        visibleClusters.sumOf { it.appearanceCount }
    }

    LaunchedEffect(Unit) {
        viewModel.generateCollage(selectedStyle)
    }

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearExportMessage()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StitchBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Collage Studio Top Header ───────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Collage Studio",
                                color = StitchOnSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StitchPrimaryContainer.copy(alpha = 0.3f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = StitchPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = "Preview & customize your cinematic layout",
                            color = StitchOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceContainerHigh)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = StitchOnSurfaceVariant, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Live Stat Pill Bar ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StitchSurfaceContainerHigh.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "✦", color = StitchSecondary, fontSize = 13.sp)
                        Text(
                            text = "${visibleClusters.size} Unique People",
                            color = StitchOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(StitchSecondary)
                        )
                        Text(
                            text = "$totalAppearances Appearances",
                            color = StitchOnSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(StitchSurfaceContainerHighest)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI Curated",
                            color = StitchPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Style Presets Tab Selector ───────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StitchSurfaceContainerLowest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CollageStyle.values().forEach { style ->
                        val isSelected = style == selectedStyle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                StitchPrimaryContainer,
                                                StitchPrimaryContainer,
                                                StitchSecondaryContainer
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable {
                                    if (!isGenerating && style != selectedStyle) {
                                        viewModel.generateCollage(style)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style.displayName,
                                color = if (isSelected) StitchOnPrimaryContainer else StitchOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Canvas Stage / Live Preview Container ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 340.dp, max = 460.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurfaceContainerLowest)
                        .border(1.dp, StitchOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isGenerating,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "collageState"
                    ) { generating ->
                        if (generating || collageBitmap == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = StitchSecondary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Rendering cinematic collage…",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = collageBitmap!!.asImageBitmap(),
                                    contentDescription = "Collage Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                // Watermark Floating Pill Overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(StitchSurfaceContainerLowest.copy(alpha = 0.85f))
                                        .border(0.5.dp, StitchOutlineVariant, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "✦", color = StitchSecondary, fontSize = 10.sp)
                                        Text(
                                            text = "FaceCollage AI · Balanced Focus",
                                            color = StitchOnSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── Style Mockup Samples (Visual Concept Switchers) ──────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "STYLE PRESETS",
                        color = StitchOnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Preset 1: Polaroid Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StitchSurfaceContainer)
                                .border(
                                    1.dp,
                                    if (selectedStyle == CollageStyle.POLAROID) StitchSecondary else StitchOutlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (!isGenerating && selectedStyle != CollageStyle.POLAROID) {
                                        viewModel.generateCollage(CollageStyle.POLAROID)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "🎞️", fontSize = 18.sp)
                                Text(
                                    text = "Polaroid Board",
                                    color = StitchOnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "White borders, tape accents & retro badges",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // Preset 2: Story Poster Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StitchSurfaceContainer)
                                .border(
                                    1.dp,
                                    if (selectedStyle == CollageStyle.STORY_POSTER) StitchSecondary else StitchOutlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (!isGenerating && selectedStyle != CollageStyle.STORY_POSTER) {
                                        viewModel.generateCollage(CollageStyle.STORY_POSTER)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "🎬", fontSize = 18.sp)
                                Text(
                                    text = "Story Poster",
                                    color = StitchOnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Editorial typography & cast credits banner",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Bottom Floating Action Shelf ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurfaceContainerHighest.copy(alpha = 0.85f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary Glass Action: Share
                    OutlinedButton(
                        onClick = { viewModel.shareCollage(context) },
                        enabled = collageBitmap != null && !isGenerating,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StitchSurfaceContainer,
                            contentColor = StitchOnSurface
                        ),
                        border = BorderStroke(1.dp, StitchOutlineVariant)
                    ) {
                        Text(text = "Share", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Primary Gradient CTA: Save / Export
                    Button(
                        onClick = { viewModel.saveCollageToGallery(context) },
                        enabled = collageBitmap != null && !isGenerating,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = StitchSecondaryContainer),
                        shape = RoundedCornerShape(14.dp),
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
                                            StitchSecondaryContainer,
                                            StitchSecondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "↓", color = StitchOnPrimaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Save Collage",
                                    color = StitchOnPrimaryContainer,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
