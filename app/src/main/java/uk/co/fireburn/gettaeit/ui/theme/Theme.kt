package uk.co.fireburn.gettaeit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ThistleDark,
    onPrimary = InkDark,
    primaryContainer = ThistleDark.copy(alpha = 0.18f),
    onPrimaryContainer = ThistleDark,
    secondary = LochDark,
    onSecondary = InkDark,
    tertiary = IrnBruDark,
    onTertiary = InkDark,
    error = RustDark,
    onError = InkDark,
    background = BgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceQuietDark,
    onSurfaceVariant = InkSoftDark,
    outline = InkFaintDark,
    outlineVariant = LineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = ThistleLight,
    onPrimary = Color.White,
    primaryContainer = ThistleLight.copy(alpha = 0.12f),
    onPrimaryContainer = ThistleLight,
    secondary = LochLight,
    onSecondary = Color.White,
    tertiary = IrnBruLight,
    onTertiary = Color.White,
    error = RustLight,
    onError = Color.White,
    background = BgLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceQuietLight,
    onSurfaceVariant = InkSoftLight,
    outline = InkFaintLight,
    outlineVariant = LineLight,
)

@Composable
fun GetTaeItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Let the app draw edge-to-edge; status bar is transparent by default
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
