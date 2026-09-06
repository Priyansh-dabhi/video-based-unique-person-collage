package com.example.video_basedunique_personcollage.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Always-dark cinematic theme — no dynamic color, no light variant.
 * This gives us full control over the premium dark aesthetic we want.
 */
private val AppColorScheme = darkColorScheme(
    primary              = StitchPrimary,
    onPrimary            = StitchOnPrimary,
    primaryContainer     = StitchPrimaryContainer,
    onPrimaryContainer   = StitchOnPrimaryContainer,
    secondary            = StitchSecondary,
    onSecondary          = StitchOnSecondary,
    secondaryContainer   = StitchSecondaryContainer,
    onSecondaryContainer = StitchOnSecondaryContainer,
    background           = StitchBackground,
    onBackground         = StitchOnSurface,
    surface              = StitchSurface,
    onSurface            = StitchOnSurface,
    surfaceVariant       = StitchSurfaceContainerHigh,
    onSurfaceVariant     = StitchOnSurfaceVariant,
    outline              = StitchOutline,
    outlineVariant       = StitchOutlineVariant,
    error                = StitchError,
    onError              = StitchOnError
)

@Composable
fun VideobasedUniquepersonCollageTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}