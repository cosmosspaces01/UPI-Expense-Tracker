package com.upi.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════
// Midnight Slate — Clean & Professional Dark Fintech Theme
// ══════════════════════════════════════════════════════════

// Core surfaces (neutral dark slate — no undertones)
val Background       = Color(0xFF0F1117)  // page background
val Surface          = Color(0xFF1A1D26)  // cards, bottom sheets
val SurfaceElevated  = Color(0xFF242832)  // modals, elevated content

// Accent (teal — the only accent color, used sparingly)
val Accent           = Color(0xFF00BFA6)  // primary CTA, active states, links
val AccentDim        = Color(0xFF00897B)  // borders, secondary buttons, muted accent

// Text hierarchy (cool pure whites)
val TextPrimary      = Color(0xFFF0F2F5)  // headings, amounts, primary content
val TextSecondary    = Color(0xFF8E95A2)  // labels, subtitles, descriptions
val TextMuted        = Color(0xFF4A5060)  // timestamps, hints, disabled text

// Semantic colors (standard, recognizable)
val DebitRed         = Color(0xFFEF5350)  // debited amounts, danger actions
val SuccessGreen     = Color(0xFF66BB6A)  // settled, credited, success
val WarningAmber     = Color(0xFFFFA726)  // budget warnings, caution

// Structural
val Divider          = Color(0xFF1E222C)  // subtle separators, card borders

// ── Backward-compatible aliases ──────────────────────────
// Maps legacy names used across files to new Midnight Slate values.
val DarkBackground      = Background
val CardBackground      = Surface
val PrimaryPurple       = Accent
val PrimaryPurpleLight  = AccentDim
val AccentBlue          = Accent
val AccentBlueMid       = AccentDim
val AccentBlueDim       = Color(0xFF1A3A36)
val PrimaryViolet       = Accent
val PrimaryPink         = Accent
val PrimaryMuted        = AccentDim
val GradientStart       = Accent
val GradientMid         = Accent
val GradientEnd         = Accent
val AccentCoral         = DebitRed
val AccentAmber         = WarningAmber
val AccentMint          = SuccessGreen
val AccentSky           = Color(0xFF5ED4F5)
val WarningRed          = DebitRed
val AccentOrange        = WarningAmber
val AccentPink          = Accent
val AccentYellow        = WarningAmber
val AccentGrey          = TextMuted
val BottomNavSelected   = Accent
val BottomNavUnselected = TextMuted
val BorderColor         = Divider
