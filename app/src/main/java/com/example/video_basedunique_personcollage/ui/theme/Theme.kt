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
    primary              = PrimaryDefault,
    onPrimary            = OnPrimary,
    primaryContainer     = PrimaryDark,
    onPrimaryContainer   = PrimaryLight,
    secondary            = SecondaryDefault,
    onSecondary          = OnSecondary,
    secondaryContainer   = Color(0xFF2B2200),
    onSecondaryContainer = SecondaryDefault,
    background           = AppBackground,
    onBackground         = OnSurface,
    surface              = SurfaceCard,
    onSurface            = OnSurface,
    surfaceVariant       = SurfaceElevated,
    onSurfaceVariant     = OnSurfaceMuted,
    outline              = Outline,
    outlineVariant       = OutlineVariant,
    error                = ErrorColor,
    onError              = OnPrimary
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