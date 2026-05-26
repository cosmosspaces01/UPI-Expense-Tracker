package com.upi.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════
// Neon Sunset — Vibrant Dark Fintech Theme
// ══════════════════════════════════════════════════════════

// Core surfaces (warm charcoal with violet undertone)
val Background       = Color(0xFF0D0B15)  // page background
val Surface          = Color(0xFF1A1625)  // cards, bottom sheets
val SurfaceElevated  = Color(0xFF252136)  // elevated cards, modals

// Primary gradient palette (Violet → Hot Pink)
val PrimaryViolet    = Color(0xFF7C5CFC)  // primary CTA, active states
val PrimaryPink      = Color(0xFFFF6B9D)  // gradient end, highlights
val PrimaryMuted     = Color(0xFF5A3FB5)  // secondary buttons, borders

// Gradient helper colors
val GradientStart    = Color(0xFF7C5CFC)  // violet
val GradientMid      = Color(0xFFE84393)  // hot pink
val GradientEnd      = Color(0xFFFF6B6B)  // coral

// Accent palette (multi-color for variety)
val AccentCoral      = Color(0xFFFF6B6B)  // warm red accent
val AccentAmber      = Color(0xFFFFB84D)  // golden amber accent
val AccentMint       = Color(0xFF4ECBA0)  // fresh mint accent
val AccentSky        = Color(0xFF5ED4F5)  // sky blue accent

// Text hierarchy (warm whites)
val TextPrimary      = Color(0xFFF5F0FF)  // headings, amounts
val TextSecondary    = Color(0xFFB8A9D4)  // labels, subtitles
val TextMuted        = Color(0xFF5A4D73)  // timestamps, hints

// Semantic colors
val DebitRed         = Color(0xFFFF6B6B)  // debited amounts
val SuccessGreen     = Color(0xFF4ECBA0)  // settled, credited
val WarningAmber     = Color(0xFFFFB84D)  // budget warnings

// Structural
val Divider          = Color(0xFF1E1A2E)  // list dividers, borders

// ── Backward-compatible aliases ──────────────────────────
// These map old names → new Neon Sunset values so existing
// code compiles without a mass search-replace of every file.
val DarkBackground      = Background
val CardBackground      = Surface
val PrimaryPurple       = PrimaryViolet
val PrimaryPurpleLight  = PrimaryMuted
val AccentBlue          = PrimaryViolet
val AccentBlueMid       = PrimaryMuted
val AccentBlueDim       = Color(0xFF3A2D6B)
val WarningRed          = DebitRed
val AccentOrange        = WarningAmber
val AccentPink          = PrimaryPink
val AccentYellow        = WarningAmber
val AccentGrey          = TextMuted
val BottomNavSelected   = PrimaryViolet
val BottomNavUnselected = TextMuted
val BorderColor         = Divider
