package com.example.video_basedunique_personcollage.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette ───────────────────────────────────────────────────────────
// Midnight blue-black background with electric violet/indigo primary accent.
// Secondary accent: warm amber for badges/stats.

// Backgrounds
val AppBackground   = Color(0xFF0A0C10)   // near-black blue
val SurfaceCard     = Color(0xFF141720)   // card / sheet surface
val SurfaceElevated = Color(0xFF1C2030)   // raised card

// Primary (indigo-violet)
val PrimaryDefault  = Color(0xFF7B68EE)   // medium-slate-blue — vibrant & unique
val PrimaryLight    = Color(0xFFADA3F7)
val PrimaryDark     = Color(0xFF4B3BC7)
val OnPrimary       = Color(0xFFFFFFFF)

// Secondary (amber)
val SecondaryDefault = Color(0xFFFFC857)  // warm amber
val SecondaryDark    = Color(0xFFE6A800)
val OnSecondary      = Color(0xFF1A1200)

// Surface / onSurface
val OnSurface       = Color(0xFFE8E8F0)
val OnSurfaceMuted  = Color(0xFF9B9DB8)
val Outline         = Color(0xFF2E3148)
val OutlineVariant  = Color(0xFF3C3F60)

// Semantic
val ErrorColor      = Color(0xFFFF6B6B)
val SuccessColor    = Color(0xFF4CAF50)

// Legacy (kept so Theme.kt compiles without changes)
val Purple80        = PrimaryLight
val PurpleGrey80    = Color(0xFFCCC2DC)
val Pink80          = Color(0xFFEFB8C8)
val Purple40        = PrimaryDefault
val PurpleGrey40    = Color(0xFF625b71)
val Pink40          = Color(0xFF7D5260)