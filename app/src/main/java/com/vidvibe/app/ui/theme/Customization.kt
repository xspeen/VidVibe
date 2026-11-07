package com.vidvibe.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Options mirrored from the "App Settings" screen of the supplied prototype. */

enum class AnimationStyle(val key: String, val label: String, val durationMs: Int) {
    Bouncy("bouncy", "Bouncy", 400),
    Snappy("snappy", "Snappy", 200),
    Smooth("smooth", "Smooth", 600);

    companion object {
        fun from(key: String?): AnimationStyle = entries.firstOrNull { it.key == key } ?: Bouncy
    }
}

enum class AppFont(
    val key: String,
    val label: String,
    val family: FontFamily
) {
    Inter("inter", "Inter", FontFamily.SansSerif),
    Playfair("playfair", "Playfair", FontFamily.Serif),
    Mono("mono", "Mono", FontFamily.Monospace);

    companion object {
        fun from(key: String?): AppFont = entries.firstOrNull { it.key == key } ?: Inter
    }
}

enum class BackgroundLook(
    val key: String,
    val label: String,
    val color: Color
) {
    Midnight("midnight", "Midnight", Color(0xFF0A0A0C)),
    Ocean("ocean", "Ocean", Color(0xFF0A1F3D)),
    Forest("forest", "Forest", Color(0xFF0A2E1D));

    companion object {
        fun from(key: String?): BackgroundLook = entries.firstOrNull { it.key == key } ?: Midnight
    }
}

enum class TextTone(
    val key: String,
    val label: String,
    val color: Color
) {
    White("white", "White", Color(0xFFF5F5F7)),
    Gold("gold", "Gold", Color(0xFFFFD700)),
    Cyan("cyan", "Cyan", Color(0xFF00E5FF)),
    Pink("pink", "Pink", Color(0xFFFF69B4)),
    Green("green", "Green", Color(0xFF30D158)),
    Orange("orange", "Orange", Color(0xFFFFA500)),
    Red("red", "Red", Color(0xFFFF453A));

    companion object {
        fun from(key: String?): TextTone = entries.firstOrNull { it.key == key } ?: White
    }
}

enum class MenuGlass(
    val key: String,
    val label: String,
    val background: Color,
    val highlight: Color
) {
    Default("default", "Default", Color(0xCC14141A), Color(0x40FFFFFF)),
    Ocean("ocean", "Ocean", Color(0xB30A2850), Color(0x6610C8FF)),
    Emerald("emerald", "Emerald", Color(0xB30A3C1E), Color(0x6610FF96)),
    Amethyst("amethyst", "Amethyst", Color(0xB3320A46), Color(0x66C864FF)),
    Ruby("ruby", "Ruby", Color(0xB3460A14), Color(0x66FF6464));

    companion object {
        fun from(key: String?): MenuGlass = entries.firstOrNull { it.key == key } ?: Default
    }
}

enum class AppThemeMode(val key: String, val label: String) {
    Dark("dark", "Dark"),
    Amoled("amoled", "AMOLED");

    companion object {
        fun from(key: String?): AppThemeMode = entries.firstOrNull { it.key == key } ?: Dark
    }
}

fun backgroundFor(look: BackgroundLook, theme: AppThemeMode): Color =
    if (theme == AppThemeMode.Amoled) Color.Black else look.color

data class MenuPalette(
    val background: Color,
    val highlight: Color
)

data class CustomizationSelection(
    val animationStyle: AnimationStyle,
    val appFont: AppFont,
    val backgroundLook: BackgroundLook,
    val textColor: TextTone,
    val glassColor: MenuGlass,
    val appTheme: AppThemeMode
)

val LocalAppTextColor = staticCompositionLocalOf { TextTone.White.color }
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
val LocalAnimationDuration = staticCompositionLocalOf { 400 }
val LocalMenuPalette = staticCompositionLocalOf {
    MenuPalette(MenuGlass.Default.background, MenuGlass.Default.highlight)
}
