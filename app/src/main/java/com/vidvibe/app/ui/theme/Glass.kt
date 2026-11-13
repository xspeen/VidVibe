package com.vidvibe.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Icon treatments for the navigation rail and the rest of the app chrome.
 * They change the stroke weight and the optional soft underlay of the custom
 * vector glyphs, not the glyph geometry, so every pack stays recognisable.
 */
enum class IconPack(val key: String, val label: String) {
    Line("line", "Line"),
    Rounded("rounded", "Rounded"),
    Bold("bold", "Bold"),
    Duotone("duotone", "Duotone");

    companion object {
        fun from(key: String?): IconPack = entries.firstOrNull { it.key == key } ?: Line
    }
}

/**
 * One derived palette for every glass surface: navigation, queue cards,
 * drawers and menus. The liquid toggle only changes the amount of light that
 * passes through the material, never the layout, so switching it never moves
 * a control.
 */
data class GlassStyle(
    val bodyTop: Color,
    val bodyBottom: Color,
    val stroke: Color,
    val sheen: Color,
    val glow: Color,
    val indicator: Color,
    val indicatorStroke: Color
) {
    companion object {
        fun from(palette: MenuPalette, liquid: Boolean): GlassStyle {
            val base = palette.background
            return GlassStyle(
                // 2.8.5: genuinely frosted glass. Panels are translucent so
                // the blurred, colour-sampled album art behind them shows
                // through, while the stroke and sheen keep text readable.
                bodyTop = base.copy(alpha = if (liquid) 0.62f else 1f),
                bodyBottom = base.copy(alpha = if (liquid) 0.48f else 1f),
                stroke = palette.highlight.copy(alpha = if (liquid) 0.22f else 0.08f),
                sheen = palette.highlight.copy(alpha = if (liquid) 0.24f else 0.08f),
                glow = palette.highlight.copy(alpha = if (liquid) 0.08f else 0.02f),
                indicator = palette.highlight.copy(alpha = if (liquid) 0.12f else 0.06f),
                indicatorStroke = palette.highlight.copy(alpha = if (liquid) 0.20f else 0.10f)
            )
        }
    }
}

val LocalGlassStyle = staticCompositionLocalOf {
    GlassStyle.from(
        MenuPalette(MenuGlass.Default.background, MenuGlass.Default.highlight),
        true
    )
}

val LocalIconPack = staticCompositionLocalOf { IconPack.Line }
