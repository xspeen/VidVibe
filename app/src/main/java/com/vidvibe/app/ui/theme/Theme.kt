package com.vidvibe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

val LocalAppBackground = staticCompositionLocalOf { VidVibeBackground }

private val VidVibeColors = darkColorScheme(
    primary = VidVibeWhite,
    onPrimary = Color.Black,
    primaryContainer = VidVibeSurface3,
    onPrimaryContainer = VidVibeText,
    secondary = VidVibeRed,
    onSecondary = VidVibeWhite,
    secondaryContainer = VidVibeSurface3,
    onSecondaryContainer = VidVibeText,
    tertiary = VidVibeSelectionBlue,
    onTertiary = Color.Black,
    background = VidVibeBackground,
    onBackground = VidVibeText,
    surface = VidVibeSurface,
    onSurface = VidVibeText,
    surfaceVariant = VidVibeSurface2,
    onSurfaceVariant = VidVibeTextSecondary,
    surfaceContainer = VidVibeSurface2,
    surfaceContainerHigh = VidVibeSurface3,
    outline = VidVibeBorder,
    outlineVariant = VidVibeBorder,
    error = VidVibeRed,
    onError = VidVibeWhite
)

@Composable
fun VidVibeTheme(
    background: Color = VidVibeBackground,
    fontScaleSp: Float = 16f,
    fontFamily: FontFamily = FontFamily.SansSerif,
    textColor: Color = VidVibeText,
    animationDurationMs: Int = 400,
    menuPalette: MenuPalette = MenuPalette(
        MenuGlass.Default.background,
        MenuGlass.Default.highlight
    ),
    content: @Composable () -> Unit
) {
    @Suppress("unused")
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val fontScale = (fontScaleSp / 16f).coerceIn(0.75f, 1.5f)

    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = fontScale),
        LocalAppBackground provides background,
        LocalAppTextColor provides textColor,
        LocalAppFontFamily provides fontFamily,
        LocalAnimationDuration provides animationDurationMs,
        LocalMenuPalette provides menuPalette
    ) {
        MaterialTheme(
            colorScheme = VidVibeColors.copy(
                background = background,
                onBackground = textColor,
                onSurface = textColor
            ),
            typography = vidVibeTypography(fontFamily),
            content = content
        )
    }
}
