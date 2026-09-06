package com.example.video_basedunique_personcollage.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.video_basedunique_personcollage.ui.components.*
import com.example.video_basedunique_personcollage.ui.theme.*

@Composable
fun ImportScreenView(
    onSelectVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Scanner sweep line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 130f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    // Pulse animation for target reticle
    val targetPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "targetPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        BrandHeader(title = "Import")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))

            // ── Ambient Atmospheric Backdrop Stage ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(StitchSurfaceContainerLow)
                    .border(1.dp, StitchOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(top = 26.dp, bottom = 22.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Badge: Cinematic Vision Engine
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StitchSurfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "✦", color = StitchSecondary, fontSize = 12.sp)
                        Text(
                            text = "CINEMATIC VISION ENGINE",
                            color = StitchSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Intelligent face detection and cinematic highlight collages from your videos",
                        color = StitchOnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 25.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // ── Central Visual Stage: Filmstrip + Scanner Motif ────────
                    Box(
                        modifier = Modifier
                            .size(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background angled filmstrip card 1 (-6 deg)
                        Box(
                            modifier = Modifier
                                .size(width = 205.dp, height = 130.dp)
                                .rotate(-6f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StitchSurfaceContainerHighest.copy(alpha = 0.5f))
                                .border(1.dp, StitchOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilmstripSprockets()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(105.dp)
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StitchSurfaceContainerLowest)
                                )
                                FilmstripSprockets()
                            }
                        }

                        // Background angled filmstrip card 2 (+5 deg)
                        Box(
                            modifier = Modifier
                                .size(width = 195.dp, height = 125.dp)
                                .rotate(5f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StitchSurfaceContainerHigh.copy(alpha = 0.75f))
                                .border(1.dp, StitchPrimaryContainer.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilmstripSprockets()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StitchSurfaceContainer)
                                )
                                FilmstripSprockets()
                            }
                        }

                        // Central Scanner Glass Viewfinder Frame
                        Box(
                            modifier = Modifier
                                .size(185.dp)
                                .shadow(16.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(StitchSurfaceContainer.copy(alpha = 0.95f))
                                .border(1.dp, StitchSecondary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(10.dp)
                        ) {
                            // Viewfinder Reticle Corners
                            HudCorner(
                                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                                isTop = true,
                                isStart = true
                            )
                            HudCorner(
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                isTop = true,
                                isStart = false
                            )
                            HudCorner(
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                                isTop = false,
                                isStart = true
                            )
                            HudCorner(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                                isTop = false,
                                isStart = false
                            )

                            // Animated Sweep Scanline
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = scanOffset.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, StitchSecondary, Color.Transparent)
                                        )
                                    )
                                    .shadow(6.dp, shape = RoundedCornerShape(1.dp), ambientColor = StitchSecondary, spotColor = StitchSecondary)
                            )

                            // Central Target Face Frame with Occurrence Pill
                            Box(
                                modifier = Modifier.align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(StitchSurfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "👤",
                                            fontSize = 32.sp
                                        )
                                    }
                                }

                                // Face Occurrence Tag Pill Overlay (e.g. ×24)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(StitchSurfaceContainerLowest)
                                        .border(1.dp, StitchSecondary.copy(alpha = 0.5f), RoundedCornerShape(50))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(text = "✦", color = StitchSecondary, fontSize = 9.sp)
                                        Text(
                                            text = "×24",
                                            color = StitchSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Telemetry metadata line positioned cleanly between corner brackets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(StitchSecondary)
                                    )
                                    Text(
                                        text = "REC 60P",
                                        color = StitchOnSurfaceVariant,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "512-D EMB",
                                    color = StitchSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── CTA & Action Sequence ──────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Primary Rich Gradient Button
                        Button(
                            onClick = onSelectVideo,
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
                                                Color(0xFF8D4BF6),
                                                StitchSecondaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "▶", color = StitchOnPrimaryContainer, fontSize = 16.sp)
                                    Text(
                                        text = "Select Video to Analyze",
                                        color = StitchOnPrimaryContainer,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = "MP4, MOV, WebM · Up to 4K 60fps",
                            color = StitchOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Diagnostic / Pro Tip Card ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StitchSurfaceContainer)
                    .border(1.dp, StitchOutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💡", fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pro Tip: Multi-Angle Coverage",
                        color = StitchOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Videos with natural lighting, stage spotlights, or varied angles yield rich cinematic facial clusters.",
                        color = StitchOnSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Trust Badge Pill ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StitchSurfaceContainerHighest.copy(alpha = 0.85f))
                    .border(1.dp, StitchOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 16.sp
                )
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Powered by FaceNet-512 AI",
                        color = StitchOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = "100% On-Device Neural Pipeline",
                        color = StitchOnSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FilmstripSprockets() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StitchSurfaceBright.copy(alpha = 0.8f))
            )
        }
    }
}
