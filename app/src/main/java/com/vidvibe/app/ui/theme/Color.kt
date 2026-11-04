package com.vidvibe.app.ui.theme

import androidx.compose.ui.graphics.Color

val VidVibeBackground = Color(0xFF050507)
val VidVibeSurface = Color(0xFF121318)
val VidVibeSurface2 = Color(0xFF1B1C24)
val VidVibeSurface3 = Color(0xFF262834)
val VidVibeSurface4 = Color(0xFF2A2B37)

val VidVibeText = Color(0xFFF5F6FA)
val VidVibeTextSecondary = Color(0xFFA0A5B5)
val VidVibeTextTertiary = Color(0xFF62677A)

val VidVibeRed = Color(0xFFE53935)
val VidVibeRedSoft = Color(0xFFEF9A9A)
val VidVibeAccentRed = Color(0xFFE57373)
val VidVibeGreen = Color(0xFF4CAF50)
val VidVibeSelectionBlue = Color(0xFF82B1FF)
val VidVibeBlue = Color(0xFF2979FF)
val VidVibeWhite = Color(0xFFFFFFFF)

val VidVibeBorder = Color(0x14FFFFFF)
val VidVibeBorderStrong = Color(0x59FFFFFF)
val VidVibeScrim = Color(0x99000000)

val BackgroundOptions = listOf(
    BackgroundOption("Midnight", Color(0xFF050507)),
    BackgroundOption("Deep Navy", Color(0xFF080D18)),
    BackgroundOption("Forest", Color(0xFF07100C)),
    BackgroundOption("Plum", Color(0xFF120A14)),
    BackgroundOption("Graphite", Color(0xFF101114)),
    BackgroundOption("Warm Black", Color(0xFF100C0A))
)

data class BackgroundOption(
    val name: String,
    val color: Color
)
