package com.vidvibe.app.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.R
import com.vidvibe.app.ui.theme.LocalMenuPalette
import com.vidvibe.app.ui.viewmodel.DownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class QualityChoice(val height: Int, val label: String, val detail: String)

private val qualities = listOf(
    QualityChoice(2160, "Best", "4K"),
    QualityChoice(1080, "1080p", "Full HD"),
    QualityChoice(720, "720p", "HD"),
    QualityChoice(480, "480p", "SD"),
    QualityChoice(360, "360p", "Data saver")
)

private val CardShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/** Brand of the shared link, detected from the URL host. */
private enum class Platform(val label: String) {
    YouTube("YouTube"),
    Instagram("Instagram"),
    TikTok("TikTok"),
    Facebook("Facebook"),
    X("X"),
    Twitter("Twitter"),
    Reddit("Reddit"),
    Vimeo("Vimeo"),
    Twitch("Twitch"),
    Threads("Threads"),
    Other("Link");

    companion object {
        fun detect(host: String): Platform {
            val h = host.lowercase()
            return when {
                h.contains("youtu") -> YouTube
                h.contains("instagram") -> Instagram
                h.contains("tiktok") -> TikTok
                h.contains("facebook") || h.contains("fb.watch") || h.contains("fb.com") -> Facebook
                h == "x.com" || h.endsWith(".x.com") -> X
                h.contains("twitter") -> Twitter
                h.contains("reddit") || h.contains("redd.it") -> Reddit
                h.contains("vimeo") -> Vimeo
                h.contains("twitch") -> Twitch
                h.contains("threads") -> Threads
                else -> Other
            }
        }
    }
}

@Composable
fun ShareSheet(
    initialLink: String,
    state: DownloadState,
    onDownload: (String, Int) -> Unit,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val palette = LocalMenuPalette.current
    val haptics = LocalHapticFeedback.current

    var link by remember { mutableStateOf(initialLink) }
    var quality by remember { mutableIntStateOf(720) }
    var hasPicked by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }

    val running = state is DownloadState.Running
    val complete = state is DownloadState.Complete

    val host = remember(link) {
        runCatching { android.net.Uri.parse(link).host.orEmpty().removePrefix("www.") }
            .getOrDefault("")
    }
    val platform = remember(host) { Platform.detect(host) }

    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(complete) {
        if (complete) {
            delay(700)
            onSaved()
        }
    }

    val scrim by animateFloatAsState(
        targetValue = if (shown) 0.62f else 0f,
        animationSpec = tween(220),
        label = "scrim"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card-scale"
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel
                )
        )

        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(CardShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.background,
                                palette.background.copy(alpha = 0.97f),
                                Color(0xF50A0A0C)
                            )
                        )
                    )
                    .border(1.dp, palette.highlight, CardShape)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.28f))
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // VidVibe's own icon, so the drawer is clearly ours.
                    // Adaptive launcher icons are XML, which painterResource
                    // cannot decode, so compose the two bitmap layers instead.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.22f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Save with VidVibe",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlatformBadge(platform = platform, size = 13.dp)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (host.isBlank()) platform.label
                                else "${platform.label}  $host",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCancel
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseGlyph(
                            color = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    BasicTextField(
                        value = link,
                        onValueChange = { if (!running) link = it },
                        singleLine = true,
                        enabled = !running,
                        textStyle = TextStyle(
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 10.5.sp
                        ),
                        cursorBrush = Brush.verticalGradient(
                            listOf(Color.White, Color.White)
                        ),
                        decorationBox = { inner ->
                            if (link.isBlank()) {
                                Text(
                                    text = "Paste a video link",
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.36f)
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "QUALITY",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.42f)
                )

                Spacer(Modifier.height(7.dp))

                QualityPicker(
                    selected = quality,
                    bubbleVisible = hasPicked,
                    paletteBackground = palette.background,
                    paletteHighlight = palette.highlight,
                    enabled = !running,
                    onPick = { picked ->
                        if (quality != picked) {
                            quality = picked
                            hasPicked = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )

                Spacer(Modifier.height(14.dp))

                when (state) {
                    is DownloadState.Running -> {
                        ProgressBlock(state.progress, state.status)
                    }

                    is DownloadState.Error -> {
                        Text(
                            text = state.message,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFFFF8A8E)
                        )
                        Spacer(Modifier.height(10.dp))
                        SlideToDownload(
                            label = "Slide to try again",
                            enabled = link.isNotBlank(),
                            highlight = palette.highlight,
                            onTrigger = { onDownload(link.trim(), quality) }
                        )
                    }

                    is DownloadState.Complete -> {
                        Text(
                            text = "Saved to your gallery",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF30D158)
                        )
                    }

                    DownloadState.Idle -> {
                        SlideToDownload(
                            label = if (link.isBlank()) "Paste a link first"
                            else "Slide to download",
                            enabled = link.isNotBlank(),
                            highlight = palette.highlight,
                            onTrigger = { onDownload(link.trim(), quality) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Saves straight to your gallery",
                    fontSize = 8.5.sp,
                    color = Color.White.copy(alpha = 0.32f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun QualityPicker(
    selected: Int,
    bubbleVisible: Boolean,
    paletteBackground: Color,
    paletteHighlight: Color,
    enabled: Boolean,
    onPick: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val count = qualities.size
        val chipWidth = (maxWidth - gap * (count - 1)) / count
        val index = qualities.indexOfFirst { it.height == selected }.coerceAtLeast(0)

        val bubbleWidth = 84.dp
        val center = chipWidth * index + gap * index + chipWidth / 2
        val targetX = (center - bubbleWidth / 2)
            .coerceIn(0.dp, (maxWidth - bubbleWidth).coerceAtLeast(0.dp))

        val bubbleX by animateDpAsState(
            targetValue = targetX,
            animationSpec = spring(
                dampingRatio = 0.62f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "quality-bubble-x"
        )
        val bubbleScale by animateFloatAsState(
            targetValue = if (bubbleVisible) 1f else 0.86f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "quality-bubble-scale"
        )
        val bubbleAlpha by animateFloatAsState(
            targetValue = if (bubbleVisible) 1f else 0f,
            animationSpec = tween(150),
            label = "quality-bubble-alpha"
        )

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = bubbleX)
                        .width(bubbleWidth)
                        .height(23.dp)
                        .graphicsLayer {
                            scaleX = bubbleScale
                            scaleY = bubbleScale
                            alpha = bubbleAlpha
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(7.dp))
                        .background(paletteBackground.copy(alpha = 0.96f))
                        .border(
                            1.dp,
                            paletteHighlight,
                            RoundedCornerShape(7.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val picked = qualities[index]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = picked.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = picked.detail,
                            fontSize = 7.5.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                qualities.forEach { choice ->
                    val active = choice.height == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (active) Color.White
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (active) Color.White
                                else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(9.dp)
                            )
                            .then(
                                if (!enabled) Modifier
                                else Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onPick(choice.height) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = choice.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) Color(0xFF08080A)
                            else Color.White.copy(alpha = 0.76f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBlock(progress: Int, status: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$progress%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0, 100) / 100f)
                    .height(5.dp)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun SlideToDownload(
    label: String,
    enabled: Boolean,
    highlight: Color,
    onTrigger: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var trackWidth by remember { mutableFloatStateOf(0f) }
    val thumb = 42.dp
    val inset = 6.dp
    val maxOffset = with(density) {
        (trackWidth - thumb.toPx() - inset.toPx() * 2f).coerceAtLeast(0f)
    }

    val fraction = if (maxOffset > 0f) (progress.value / maxOffset).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .onSizeChanged { trackWidth = it.width.toFloat() }
            .clip(RoundedCornerShape(27.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.07f else 0.035f))
            .border(
                1.dp,
                if (enabled) highlight else Color.White.copy(alpha = 0.09f),
                RoundedCornerShape(27.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f * fraction),
                            Color.White.copy(alpha = 0.05f * fraction)
                        )
                    )
                )
        )

        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = Color.White.copy(alpha = (1f - fraction) * 0.65f + 0.15f),
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (progress.value + with(density) { inset.toPx() }).roundToInt(),
                        with(density) { inset.toPx() }.roundToInt()
                    )
                }
                .size(thumb)
                .clip(RoundedCornerShape(21.dp))
                .background(
                    if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
                )
                .pointerInput(enabled, maxOffset) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (maxOffset > 0f && progress.value >= maxOffset * 0.72f) {
                                    progress.animateTo(maxOffset, tween(120))
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTrigger()
                                    progress.snapTo(0f)
                                } else {
                                    progress.animateTo(0f, tween(180))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { progress.animateTo(0f, tween(180)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                progress.snapTo(
                                    (progress.value + dragAmount).coerceIn(0f, maxOffset)
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            ArrowGlyph(
                color = if (enabled) Color(0xFF08080A) else Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PlatformBadge(platform: Platform, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        when (platform) {
            Platform.YouTube -> youtubeGlyph()
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

private fun DrawScope.youtubeGlyph() {
    val w = size.width
    val h = size.height
    drawRoundRect(Color(0xFFFF0033), cornerRadius = CornerRadius(w * 0.24f))
    val play = Path().apply {
        moveTo(w * 0.41f, h * 0.31f)
        lineTo(w * 0.41f, h * 0.69f)
        lineTo(w * 0.71f, h * 0.50f)
        close()
    }
    drawPath(play, Color.White)
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
    drawCircle(
        color = Color.White,
        radius = w * 0.05f,
        center = Offset(w * 0.68f, h * 0.32f)
    )
}

private fun DrawScope.tiktokGlyph() {
    val w = size.width
    val h = size.height
    drawRoundRect(Color(0xFF060608), cornerRadius = CornerRadius(w * 0.24f))
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
        style = Stroke(
            width = w * 0.105f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.facebookGlyph() {
    val w = size.width
    val h = size.height
    drawRoundRect(Color(0xFF1877F2), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFF0A0A0A), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFF1DA1F2), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFFFF4500), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFF1AB7EA), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFF9146FF), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFF0A0A0A), cornerRadius = CornerRadius(w * 0.24f))
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
    drawRoundRect(Color(0xFF1C1C22), cornerRadius = CornerRadius(w * 0.24f))
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

@Composable
private fun CloseGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        drawLine(
            color,
            Offset(0f, 0f),
            Offset(size.width, size.height),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(size.width, 0f),
            Offset(0f, size.height),
            stroke,
            StrokeCap.Round
        )
    }
}

@Composable
private fun ArrowGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val stroke = Stroke(
            width = 2.2f * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawLine(color, Offset(12f * s, 4f * s), Offset(12f * s, 16f * s), 2.2f * s, StrokeCap.Round)
        drawLine(color, Offset(7f * s, 11f * s), Offset(12f * s, 16f * s), 2.2f * s, StrokeCap.Round)
        drawLine(color, Offset(12f * s, 16f * s), Offset(17f * s, 11f * s), 2.2f * s, StrokeCap.Round)
        val tray = Path().apply {
            moveTo(5f * s, 19f * s)
            lineTo(19f * s, 19f * s)
        }
        drawPath(tray, color, style = stroke)
    }
}
