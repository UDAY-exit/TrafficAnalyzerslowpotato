package com.trafficanalyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary            = CyberGreen,
    onPrimary          = DarkBackground,
    primaryContainer   = CardDark,
    onPrimaryContainer = TextPrimary,
    secondary          = AccentBlue,
    onSecondary        = DarkBackground,
    background         = DarkBackground,
    onBackground       = TextPrimary,
    surface            = SurfaceDark,
    onSurface          = TextPrimary,
    surfaceVariant     = CardDark,
    onSurfaceVariant   = TextSecondary,
    error              = ErrorRed,
    onError            = DarkBackground
)

/** Always dark theme — cybersecurity-style app. */
@Composable
fun TrafficAnalyzerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
