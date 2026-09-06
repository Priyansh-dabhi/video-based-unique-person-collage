package com.example.video_basedunique_personcollage.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                .background(AppBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Collage Studio ✨",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface
                        )
                        Text(
                            "Preview & customize your collage",
                            fontSize = 12.sp,
                            color = OnSurfaceMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 20.sp, color = OnSurfaceMuted)
                    }
                }

                // ── Style selector ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CollageStyle.values().forEach { style ->
                        val isSelected = style == selectedStyle
                        StyleChip(
                            label = style.displayName,
                            selected = isSelected,
                            enabled = !isGenerating,
                            onClick = {
                                if (!isGenerating && style != selectedStyle) {
                                    viewModel.generateCollage(style)
                                }
                            }
                        )
                    }
                }

                Divider(color = OutlineVariant, thickness = 1.dp)

                // ── Collage preview ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF07080A)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isGenerating,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "collageContent"
                    ) { generating ->
                        if (generating || collageBitmap == null) {
                            GeneratingPlaceholder()
                        } else {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    bitmap = collageBitmap!!.asImageBitmap(),
                                    contentDescription = "Collage Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                // ── Action bar ─────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveCollageToGallery(context) },
                            enabled = !isGenerating && collageBitmap != null,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDefault)
                        ) {
                            Text(
                                "⬇  Save",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareCollage(context) },
                            enabled = !isGenerating && collageBitmap != null,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, PrimaryDefault),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDefault)
                        ) {
                            Text("↗  Share", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Text(
                        text = "Saved to Pictures / UniquePersonCollage",
                        fontSize = 11.sp,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PrimaryDefault else SurfaceElevated,
        border = if (selected) null else BorderStroke(1.dp, OutlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else OnSurfaceMuted
        )
    }
}

@Composable
private fun GeneratingPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = PrimaryDefault, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text("Rendering collage…", color = OnSurfaceMuted, fontSize = 14.sp)
    }
}
