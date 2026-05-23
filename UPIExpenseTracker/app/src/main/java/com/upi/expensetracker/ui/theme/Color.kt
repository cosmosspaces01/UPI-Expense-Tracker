package com.upi.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════
// Arctic Blue — Premium Dark Fintech Theme
// ══════════════════════════════════════════════════════════

// Core surfaces
val Background       = Color(0xFF080C14)  // page background
val Surface          = Color(0xFF0F1825)  // cards, bottom sheets
val SurfaceElevated  = Color(0xFF162030)  // elevated cards, modals

// Accent palette
val AccentBlue       = Color(0xFF378ADD)  // primary CTA, active states
val AccentBlueMid    = Color(0xFF185FA5)  // secondary buttons, borders
val AccentBlueDim    = Color(0xFF0C447C)  // subtle accents, dividers

// Text hierarchy
val TextPrimary      = Color(0xFFFFFFFF)  // headings, amounts
val TextSecondary    = Color(0xFFA0C0E8)  // labels, subtitles
val TextMuted        = Color(0xFF3A5A80)  // timestamps, hints

// Semantic colors
val DebitRed         = Color(0xFFFF6B6B)  // debited amounts
val SuccessGreen     = Color(0xFF4ECBA0)  // settled, credited
val WarningAmber     = Color(0xFFFAC75F)  // budget warnings

// Structural
val Divider          = Color(0xFF0D1E35)  // list dividers, borders

// ── Backward-compatible aliases ──────────────────────────
// These map old names → new Arctic Blue values so existing
// code compiles without a mass search-replace of every file.
val DarkBackground      = Background
val CardBackground      = Surface
val PrimaryPurple       = AccentBlue
val PrimaryPurpleLight  = AccentBlueMid
val WarningRed          = DebitRed
val AccentOrange        = WarningAmber
val AccentPink          = Color(0xFFE84393)
val AccentYellow        = WarningAmber
val AccentCoral         = DebitRed
val AccentGrey          = TextMuted
val BottomNavSelected   = AccentBlue
val BottomNavUnselected = TextMuted
val BorderColor         = Divider
