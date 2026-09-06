package com.example.video_basedunique_personcollage.ui.theme

import androidx.compose.ui.graphics.Color

// ── Stitch "FaceCollage AI Studio" Design System Tokens ─────────────────────────
// Palette: Cinematic Luxe Dark (Obsidian Navy, Electric Indigo/Violet, Warm Amber)

val StitchBackground             = Color(0xFF051424) // Deep obsidian navy
val StitchSurface                = Color(0xFF051424)
val StitchSurfaceContainerLowest = Color(0xFF010F1F)
val StitchSurfaceContainerLow    = Color(0xFF0D1C2D)
val StitchSurfaceContainer       = Color(0xFF122131) // Card base
val StitchSurfaceContainerHigh   = Color(0xFF1C2B3C) // Elevated surface
val StitchSurfaceContainerHighest= Color(0xFF273647) // Highlighted card / tag
val StitchSurfaceBright          = Color(0xFF2C3A4C)

val StitchPrimary                = Color(0xFFC7BFFF) // Soft luminous violet
val StitchPrimaryContainer       = Color(0xFF5E43F3) // Deep electric violet
val StitchPrimaryFixed           = Color(0xFFE4DFFF)
val StitchOnPrimary              = Color(0xFF2A009F)
val StitchOnPrimaryContainer     = Color(0xFFE4DEFF)

val StitchSecondary              = Color(0xFFFFB95F) // Warm golden amber
val StitchSecondaryContainer     = Color(0xFFEE9800) // Deep gold / orange
val StitchOnSecondary            = Color(0xFF472A00)
val StitchOnSecondaryContainer   = Color(0xFF5B3800)

val StitchOnSurface              = Color(0xFFD4E4FA) // Luminous ice-white
val StitchOnSurfaceVariant       = Color(0xFFC8C4D9) // Muted lavender-grey
val StitchOutline                = Color(0xFF928EA2)
val StitchOutlineVariant         = Color(0xFF474556)

val StitchError                  = Color(0xFFFFB4AB)
val StitchErrorContainer         = Color(0xFF93000A)
val StitchOnError                = Color(0xFF690005)
val StitchSuccess                = Color(0xFF4CAF50)

// ── Backward-compatible Aliases ───────────────────────────────────────────────
val AppBackground    = StitchBackground
val SurfaceCard      = StitchSurfaceContainer
val SurfaceElevated  = StitchSurfaceContainerHigh
val PrimaryDefault   = StitchPrimaryContainer
val PrimaryLight     = StitchPrimary
val PrimaryDark      = Color(0xFF3F0FD6)
val OnPrimary        = Color(0xFFFFFFFF)
val SecondaryDefault = StitchSecondary
val SecondaryDark    = StitchSecondaryContainer
val OnSecondary      = StitchOnSecondary
val OnSurface        = StitchOnSurface
val OnSurfaceMuted   = StitchOnSurfaceVariant
val Outline          = StitchOutline
val OutlineVariant   = StitchOutlineVariant
val ErrorColor       = StitchError
val SuccessColor     = StitchSuccess

val Purple80         = PrimaryLight
val PurpleGrey80     = Color(0xFFCCC2DC)
val Pink80           = Color(0xFFEFB8C8)
val Purple40         = PrimaryDefault
val PurpleGrey40     = Color(0xFF625b71)
val Pink40           = Color(0xFF7D5260)