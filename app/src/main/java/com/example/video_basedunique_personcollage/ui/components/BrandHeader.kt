package com.example.video_basedunique_personcollage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.video_basedunique_personcollage.ui.theme.*

@Composable
fun BrandHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StitchSurface.copy(alpha = 0.92f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Brand Icon glyph with glowing gradient ring
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(StitchPrimaryContainer, StitchSecondaryContainer)
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(StitchSecondary, StitchPrimary)),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    color = StitchOnPrimaryContainer,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "FACECOLLAGE AI",
                    color = StitchSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = title,
                    color = StitchOnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
            }
        }

        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
fun HudCorner(
    modifier: Modifier,
    isTop: Boolean,
    isStart: Boolean,
    color: Color = StitchSecondary,
    size: androidx.compose.ui.unit.Dp = 13.dp,
    thickness: androidx.compose.ui.unit.Dp = 2.dp
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thickness)
                .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(thickness)
                .fillMaxHeight()
                .align(if (isStart) Alignment.CenterStart else Alignment.CenterEnd)
                .background(color)
        )
    }
}
