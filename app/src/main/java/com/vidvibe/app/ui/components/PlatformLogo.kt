package com.vidvibe.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vidvibe.app.data.Platform

/** Brand mark used by the queue cards, the link field and the audio list. */
@Composable
fun PlatformLogo(
    platform: Platform,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(size)) {
        when (platform) {
            Platform.YouTube -> youtubeGlyph()
            Platform.YouTubeMusic -> youtubeMusicGlyph()
            Platform.Spotify -> spotifyGlyph()
            Platform.AppleMusic -> appleMusicGlyph()
            Platform.SoundCloud -> soundCloudGlyph()
            Platform.Deezer -> deezerGlyph()
            Platform.Tidal -> tidalGlyph()
            Platform.Bandcamp -> bandcampGlyph()
            Platform.Audiomack -> audiomackGlyph()
            Platform.Instagram -> instagramGlyph()
            Platform.TikTok -> tiktokGlyph()
            Platform.Facebook -> facebookGlyph()
            Platform.X -> xGlyph()
            Platform.Twitter -> twitterGlyph()
            Platform.Reddit -> redditGlyph()
            Platform.Vimeo -> vimeoGlyph()
            Platform.Twitch -> twitchGlyph()
            Platform.Threads -> threadsGlyph()
            Platform.Other -> linkGlyph()
        }
    }
}

private fun DrawScope.base(color: Color, w: Float): Unit {
    drawRoundRect(color, cornerRadius = CornerRadius(w * 0.24f))
}

private fun DrawScope.youtubeGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFFFF0033), w)
    val play = Path().apply {
        moveTo(w * 0.41f, h * 0.31f)
        lineTo(w * 0.41f, h * 0.69f)
        lineTo(w * 0.71f, h * 0.50f)
        close()
    }
    drawPath(play, Color.White)
}

private fun DrawScope.youtubeMusicGlyph() {
    val w = size.width
    val h = size.height
    drawCircle(Color(0xFFFF0033), radius = w * 0.5f, center = Offset(w * 0.5f, h * 0.5f))
    drawCircle(Color.White, radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.5f))
    val play = Path().apply {
        moveTo(w * 0.44f, h * 0.37f)
        lineTo(w * 0.44f, h * 0.63f)
        lineTo(w * 0.64f, h * 0.50f)
        close()
    }
    drawPath(play, Color(0xFFFF0033))
}

private fun DrawScope.spotifyGlyph() {
    val w = size.width
    val h = size.height
    drawCircle(Color(0xFF1DB954), radius = w * 0.5f, center = Offset(w * 0.5f, h * 0.5f))
    val stroke = w * 0.075f
    for (index in 0..2) {
        val inset = w * (0.20f + index * 0.075f)
        val y = h * (0.40f + index * 0.13f)
        val arc = Path().apply {
            moveTo(inset, y)
            quadraticBezierTo(w * 0.5f, y - h * 0.12f, w - inset, y + h * 0.03f)
        }
        drawPath(
            arc,
            Color(0xFF0A0A0A),
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.appleMusicGlyph() {
    val w = size.width
    val h = size.height
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFFFF5A7A), Color(0xFFB5179E)),
            start = Offset.Zero,
            end = Offset(w, h)
        ),
        cornerRadius = CornerRadius(w * 0.24f)
    )
    val stemX = w * 0.60f
    drawLine(
        Color.White,
        Offset(stemX, h * 0.28f),
        Offset(stemX, h * 0.66f),
        w * 0.07f,
        StrokeCap.Round
    )
    drawCircle(Color.White, radius = w * 0.12f, center = Offset(w * 0.47f, h * 0.68f))
    drawCircle(Color.White, radius = w * 0.10f, center = Offset(w * 0.69f, h * 0.64f))
    drawLine(
        Color.White,
        Offset(stemX, h * 0.28f),
        Offset(w * 0.75f, h * 0.23f),
        w * 0.07f,
        StrokeCap.Round
    )
}

private fun DrawScope.soundCloudGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFFFF5500), w)
    val bars = 4
    for (index in 0 until bars) {
        val x = w * (0.22f + index * 0.09f)
        val height = h * (0.20f + index * 0.05f)
        drawLine(
            Color.White,
            Offset(x, h * 0.62f),
            Offset(x, h * 0.62f - height),
            w * 0.045f,
            StrokeCap.Round
        )
    }
    drawCircle(Color.White, radius = w * 0.16f, center = Offset(w * 0.70f, h * 0.52f))
    drawRoundRect(
        Color.White,
        topLeft = Offset(w * 0.58f, h * 0.52f),
        size = Size(w * 0.22f, h * 0.16f)
    )
}

private fun DrawScope.deezerGlyph() {
    val w = size.width
    val h = size.height
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFFA238FF), Color(0xFFFF3E8E)),
            start = Offset.Zero,
            end = Offset(w, h)
        ),
        cornerRadius = CornerRadius(w * 0.24f)
    )
    val rows = listOf(0.30f to 0.30f, 0.44f to 0.46f, 0.58f to 0.62f, 0.72f to 0.78f)
    rows.forEachIndexed { index, (y, end) ->
        drawLine(
            Color.White,
            Offset(w * 0.26f, h * y),
            Offset(w * end, h * y),
            w * 0.075f,
            StrokeCap.Round
        )
    }
}

private fun DrawScope.tidalGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF0A0A0A), w)
    val diamond = Path().apply {
        moveTo(w * 0.5f, h * 0.24f)
        lineTo(w * 0.68f, h * 0.42f)
        lineTo(w * 0.5f, h * 0.60f)
        lineTo(w * 0.32f, h * 0.42f)
        close()
    }
    drawPath(diamond, Color.White)
}

private fun DrawScope.bandcampGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF1DA0C3), w)
    val wave = Path().apply {
        moveTo(w * 0.20f, h * 0.60f)
        cubicTo(w * 0.34f, h * 0.32f, w * 0.48f, h * 0.72f, w * 0.62f, h * 0.44f)
        cubicTo(w * 0.70f, h * 0.28f, w * 0.76f, h * 0.34f, w * 0.80f, h * 0.40f)
    }
    drawPath(
        wave,
        Color.White,
        style = Stroke(w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.audiomackGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFFFFA200), w)
    val letter = Path().apply {
        moveTo(w * 0.30f, h * 0.72f)
        lineTo(w * 0.50f, h * 0.28f)
        lineTo(w * 0.70f, h * 0.72f)
    }
    drawPath(
        letter,
        Color.White,
        style = Stroke(w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawLine(
        Color.White,
        Offset(w * 0.38f, h * 0.56f),
        Offset(w * 0.62f, h * 0.56f),
        w * 0.06f,
        StrokeCap.Round
    )
}

private fun DrawScope.instagramGlyph() {
    val w = size.width
    val h = size.height
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFFFFC53D), Color(0xFFFF3B5C), Color(0xFF9B36C8)),
            start = Offset.Zero,
            end = Offset(w, h)
        ),
        cornerRadius = CornerRadius(w * 0.26f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.24f, h * 0.24f),
        size = Size(w * 0.52f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.16f),
        style = Stroke(w * 0.075f)
    )
    drawCircle(
        color = Color.White,
        radius = w * 0.115f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(w * 0.075f)
    )
    drawCircle(Color.White, radius = w * 0.05f, center = Offset(w * 0.68f, h * 0.32f))
}

private fun DrawScope.tiktokGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF060608), w)
    noteGlyph(w, h, Offset(w * -0.02f, h * 0.015f), Color(0xFF25F4EE))
    noteGlyph(w, h, Offset(w * 0.02f, h * -0.015f), Color(0xFFFE2C55))
    noteGlyph(w, h, Offset.Zero, Color.White)
}

private fun DrawScope.noteGlyph(w: Float, h: Float, shift: Offset, color: Color) {
    val cx = w * 0.36f + shift.x
    val cy = h * 0.69f + shift.y
    drawCircle(color, radius = w * 0.135f, center = Offset(cx, cy))
    val stem = Path().apply {
        moveTo(cx + w * 0.135f, cy)
        lineTo(cx + w * 0.135f, h * 0.24f + shift.y)
        cubicTo(
            cx + w * 0.30f, h * 0.25f + shift.y,
            cx + w * 0.34f, h * 0.34f + shift.y,
            cx + w * 0.35f, h * 0.43f + shift.y
        )
    }
    drawPath(
        stem,
        color,
        style = Stroke(w * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.facebookGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF1877F2), w)
    val f = Path().apply {
        moveTo(w * 0.60f, h * 0.20f)
        lineTo(w * 0.52f, h * 0.20f)
        cubicTo(w * 0.43f, h * 0.20f, w * 0.38f, h * 0.27f, w * 0.38f, h * 0.37f)
        lineTo(w * 0.38f, h * 0.44f)
        lineTo(w * 0.29f, h * 0.44f)
        lineTo(w * 0.29f, h * 0.56f)
        lineTo(w * 0.38f, h * 0.56f)
        lineTo(w * 0.38f, h * 0.82f)
        lineTo(w * 0.52f, h * 0.82f)
        lineTo(w * 0.52f, h * 0.56f)
        lineTo(w * 0.63f, h * 0.56f)
        lineTo(w * 0.66f, h * 0.44f)
        lineTo(w * 0.52f, h * 0.44f)
        lineTo(w * 0.52f, h * 0.39f)
        cubicTo(w * 0.52f, h * 0.34f, w * 0.55f, h * 0.32f, w * 0.60f, h * 0.32f)
        close()
    }
    drawPath(f, Color.White)
}

private fun DrawScope.xGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF0A0A0A), w)
    val stroke = w * 0.10f
    drawLine(
        Color.White,
        Offset(w * 0.28f, h * 0.27f),
        Offset(w * 0.72f, h * 0.73f),
        stroke,
        StrokeCap.Round
    )
    drawLine(
        Color.White,
        Offset(w * 0.72f, h * 0.27f),
        Offset(w * 0.28f, h * 0.73f),
        stroke,
        StrokeCap.Round
    )
}

private fun DrawScope.twitterGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF1DA1F2), w)
    val bird = Path().apply {
        moveTo(w * 0.22f, h * 0.30f)
        lineTo(w * 0.43f, h * 0.53f)
        lineTo(w * 0.23f, h * 0.74f)
        lineTo(w * 0.32f, h * 0.74f)
        lineTo(w * 0.47f, h * 0.56f)
        lineTo(w * 0.62f, h * 0.74f)
        lineTo(w * 0.80f, h * 0.74f)
        lineTo(w * 0.55f, h * 0.47f)
        lineTo(w * 0.76f, h * 0.26f)
        lineTo(w * 0.67f, h * 0.26f)
        lineTo(w * 0.51f, h * 0.44f)
        lineTo(w * 0.37f, h * 0.26f)
        close()
    }
    drawPath(bird, Color.White)
}

private fun DrawScope.redditGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFFFF4500), w)
    drawCircle(Color.White, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.56f))
    drawCircle(Color(0xFF1A1A1A), radius = w * 0.035f, center = Offset(w * 0.42f, h * 0.54f))
    drawCircle(Color(0xFF1A1A1A), radius = w * 0.035f, center = Offset(w * 0.58f, h * 0.54f))
    drawCircle(Color.White, radius = w * 0.055f, center = Offset(w * 0.68f, h * 0.34f))
    drawCircle(Color(0xFF1A1A1A), radius = w * 0.025f, center = Offset(w * 0.68f, h * 0.34f))
    drawLine(
        Color.White,
        Offset(w * 0.55f, h * 0.30f),
        Offset(w * 0.68f, h * 0.34f),
        w * 0.04f,
        StrokeCap.Round
    )
    val smile = Path().apply {
        moveTo(w * 0.43f, h * 0.63f)
        quadraticBezierTo(w * 0.50f, h * 0.70f, w * 0.57f, h * 0.63f)
    }
    drawPath(smile, Color(0xFF1A1A1A), style = Stroke(w * 0.035f, cap = StrokeCap.Round))
}

private fun DrawScope.vimeoGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF1AB7EA), w)
    val v = Path().apply {
        moveTo(w * 0.24f, h * 0.36f)
        cubicTo(w * 0.34f, h * 0.32f, w * 0.40f, h * 0.40f, w * 0.45f, h * 0.56f)
        cubicTo(w * 0.50f, h * 0.40f, w * 0.56f, h * 0.30f, w * 0.68f, h * 0.34f)
        cubicTo(w * 0.80f, h * 0.38f, w * 0.68f, h * 0.62f, w * 0.50f, h * 0.72f)
        cubicTo(w * 0.36f, h * 0.60f, w * 0.28f, h * 0.46f, w * 0.24f, h * 0.36f)
        close()
    }
    drawPath(v, Color.White)
}

private fun DrawScope.twitchGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF9146FF), w)
    val bubble = Path().apply {
        moveTo(w * 0.26f, h * 0.22f)
        lineTo(w * 0.74f, h * 0.22f)
        lineTo(w * 0.74f, h * 0.58f)
        lineTo(w * 0.60f, h * 0.72f)
        lineTo(w * 0.46f, h * 0.72f)
        lineTo(w * 0.34f, h * 0.82f)
        lineTo(w * 0.34f, h * 0.72f)
        lineTo(w * 0.26f, h * 0.72f)
        close()
    }
    drawPath(bubble, Color.White)
    drawLine(
        Color(0xFF9146FF),
        Offset(w * 0.44f, h * 0.34f),
        Offset(w * 0.44f, h * 0.56f),
        w * 0.07f,
        StrokeCap.Round
    )
    drawLine(
        Color(0xFF9146FF),
        Offset(w * 0.60f, h * 0.34f),
        Offset(w * 0.60f, h * 0.56f),
        w * 0.07f,
        StrokeCap.Round
    )
}

private fun DrawScope.threadsGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF0A0A0A), w)
    drawArc(
        color = Color.White,
        startAngle = -50f,
        sweepAngle = 300f,
        useCenter = false,
        topLeft = Offset(w * 0.26f, h * 0.24f),
        size = Size(w * 0.48f, h * 0.52f),
        style = Stroke(w * 0.075f, cap = StrokeCap.Round)
    )
    drawLine(
        Color.White,
        Offset(w * 0.50f, h * 0.50f),
        Offset(w * 0.50f, h * 0.74f),
        w * 0.075f,
        StrokeCap.Round
    )
}

private fun DrawScope.linkGlyph() {
    val w = size.width
    val h = size.height
    base(Color(0xFF1C1C22), w)
    drawCircle(
        color = Color.White,
        radius = w * 0.26f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(w * 0.06f)
    )
    drawLine(
        Color.White,
        Offset(w * 0.24f, h * 0.50f),
        Offset(w * 0.76f, h * 0.50f),
        w * 0.06f
    )
    drawOval(
        color = Color.White,
        topLeft = Offset(w * 0.38f, h * 0.24f),
        size = Size(w * 0.24f, h * 0.52f),
        style = Stroke(w * 0.06f)
    )
}
