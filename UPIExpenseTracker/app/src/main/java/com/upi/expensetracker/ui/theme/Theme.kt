package com.upi.expensetracker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ══════════════════════════════════════════════════════════
// Neon Sunset Material 3 Dark Color Scheme
// ══════════════════════════════════════════════════════════

private val NeonSunsetScheme = darkColorScheme(
    primary        = PrimaryViolet,
    onPrimary      = TextPrimary,
    primaryContainer   = PrimaryMuted,
    onPrimaryContainer = TextPrimary,
    secondary      = AccentMint,
    onSecondary    = Background,
    tertiary       = AccentAmber,
    background     = Background,
    onBackground   = TextPrimary,
    surface        = Surface,
    onSurface      = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error          = DebitRed,
    onError        = TextPrimary,
    outline        = Divider,
    outlineVariant = PrimaryMuted
)

@Composable
fun UPIExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = NeonSunsetScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar and navigation bar match the page background
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            // Use light (white) icons on the dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
