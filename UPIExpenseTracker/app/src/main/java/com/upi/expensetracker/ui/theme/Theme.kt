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
// Midnight Slate — Material 3 Dark Color Scheme
// ══════════════════════════════════════════════════════════

private val MidnightSlateScheme = darkColorScheme(
    primary            = Accent,
    onPrimary          = Background,
    primaryContainer   = AccentDim,
    onPrimaryContainer = TextPrimary,
    secondary          = AccentDim,
    onSecondary        = TextPrimary,
    tertiary           = WarningAmber,
    background         = Background,
    onBackground       = TextPrimary,
    surface            = Surface,
    onSurface          = TextPrimary,
    surfaceVariant     = SurfaceElevated,
    onSurfaceVariant   = TextSecondary,
    error              = DebitRed,
    onError            = TextPrimary,
    outline            = Divider,
    outlineVariant     = AccentDim
)

@Composable
fun UPIExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = MidnightSlateScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
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
