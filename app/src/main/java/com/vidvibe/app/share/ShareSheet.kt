package com.vidvibe.app.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.R
import com.vidvibe.app.ui.theme.LocalMenuPalette
import com.vidvibe.app.ui.viewmodel.DownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class QualityChoice(val height: Int, val label: String)

private val qualities = listOf(
    QualityChoice(2160, "Best"),
    QualityChoice(1080, "1080p"),
    QualityChoice(720, "720p"),
    QualityChoice(480, "480p"),
    QualityChoice(360, "360p")
)

private val CardShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val Green = Color(0xFF30D158)

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
    Other("Video link");

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
    val scroll = rememberScrollState()

    var link by remember { mutableStateOf(initialLink) }
    var quality by remember { mutableIntStateOf(720) }
    var shown by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    val running = state is DownloadState.Running
    val complete = state is DownloadState.Complete

    val host = remember(link) {
        runCatching { android.net.Uri.parse(link).host.orEmpty().removePrefix("www.") }
            .getOrDefault("")
    }
    val platform = remember(host) { Platform.detect(host) }

    fun dismiss() {
        if (!closing) closing = true
    }

    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(closing) {
        if (closing) {
            shown = false
            delay(700)
            onCancel()
        }
    }
    LaunchedEffect(complete) {
        if (complete) {
            // Hold the success tick long enough to read: sparkles, glow and
            // check draw slowly, then the drawer slides down in slow motion.
            delay(3000)
            shown = false
            delay(940)
            onSaved()
        }
    }

    val scrim by animateFloatAsState(
        targetValue = if (shown) 0.62f else 0f,
        animationSpec = tween(420),
        label = "scrim"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.95f,
        animationSpec = tween(380, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label = "card-scale"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardMaxHeight = maxHeight * 0.92f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismiss() }
                )
        )

        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 400,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 900,
                    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                )
            ) + fadeOut(tween(760)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = cardMaxHeight)
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
            ) {
                Column(
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                        .verticalScroll(scroll)
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
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlatformBadge(platform = platform, size = 14.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = platform.label,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.66f)
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
                                    onClick = { dismiss() }
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
                            onValueChange = { if (!running && !complete) link = it },
                            singleLine = true,
                            enabled = !running && !complete,
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
                        enabled = !running && !complete,
                        onPick = { picked ->
                            if (quality != picked) {
                                quality = picked
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    val captionAlpha by animateFloatAsState(
                        targetValue = if (complete) 0f else 1f,
                        animationSpec = tween(200),
                        label = "caption-alpha"
                    )

                    if (state is DownloadState.Error) {
                        Text(
                            text = state.message,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFFFF8A8E)
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    DownloadWindow(
                        state = state,
                        label = when {
                            state is DownloadState.Error -> "Slide to try again"
                            link.isBlank() -> "Paste a link first"
                            else -> "Slide to download"
                        },
                        enabled = link.isNotBlank() &&
                            (state is DownloadState.Idle || state is DownloadState.Error),
                        onTrigger = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDownload(link.trim(), quality)
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (running) {
                            AnalyzingGlyph(
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (complete) "Saved to your gallery"
                                else "Saves straight to your gallery",
                                fontSize = 8.5.sp,
                                color = Color.White.copy(alpha = 0.32f * captionAlpha),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quality row shaped like the bottom navigation: one rectangular glass cell
 * that springs between the labels instead of a bubble floating above them.
 */
@Composable
private fun QualityPicker(
    selected: Int,
    enabled: Boolean,
    onPick: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val count = qualities.size
        val chipWidth = (maxWidth - gap * (count - 1)) / count
        val index = qualities.indexOfFirst { it.height == selected }.coerceAtLeast(0)

        val indicatorX by animateDpAsState(
            targetValue = (chipWidth + gap) * index,
            animationSpec = spring(
                dampingRatio = 0.74f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "quality-indicator"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .width(chipWidth)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.40f),
                        RoundedCornerShape(10.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                qualities.forEach { choice ->
                    val active = choice.height == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
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
                            color = if (active) Color.White
                            else Color.White.copy(alpha = 0.58f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The delete-flow window, reused for downloading: one 80 dp glass track with
 * the 68 dp round handle and the pulsing label. Reaching the end hands the
 * same window over to the green loader, so the bar grows inside it, and the
 * finished download pops its tick with green sparks in that same window.
 */
@Composable
private fun DownloadWindow(
    state: DownloadState,
    label: String,
    enabled: Boolean,
    onTrigger: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val palette = LocalMenuPalette.current

    val running = state is DownloadState.Running
    val complete = state is DownloadState.Complete
    val target = when (state) {
        is DownloadState.Running -> state.progress / 100f
        is DownloadState.Complete -> 1f
        else -> 0f
    }

    val fill by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = if (running) 140 else 420,
            easing = if (running) LinearEasing else CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "download-fill"
    )
    val slideAlpha by animateFloatAsState(
        targetValue = if (running || complete) 0f else 1f,
        animationSpec = tween(260),
        label = "slide-alpha"
    )
    val loaderAlpha by animateFloatAsState(
        targetValue = if (running || complete) 1f else 0f,
        animationSpec = tween(260),
        label = "loader-alpha"
    )
    val percentAlpha by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = tween(320),
        label = "percent-alpha"
    )

    val insetPx = with(density) { 6.dp.toPx() }
    val handlePx = with(density) { 68.dp.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val maxOffset = (trackWidthPx - handlePx - insetPx * 2f).coerceAtLeast(1f)

    val settle = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val offset = if (dragging) dragValue else settle.value

    LaunchedEffect(running, complete) {
        if (!running && !complete) {
            dragging = false
            dragValue = 0f
            settle.snapTo(0f)
        }
    }

    val pulse by rememberInfiniteTransition(label = "slide-pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slide-pulse-value"
    )
    val pulseAlpha = 0.4f + pulse * 0.4f
    val dragFade = (1f - offset / (maxOffset * 0.5f)).coerceIn(0f, 1f)
    val labelAlpha = pulseAlpha * dragFade * slideAlpha

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .onSizeChanged { trackWidthPx = it.width }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            palette.background,
                            palette.background.copy(
                                alpha = (palette.background.alpha * 0.85f).coerceIn(0f, 1f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            palette.highlight,
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .graphicsLayer { alpha = loaderAlpha }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .background(Green)
            )
        }

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = labelAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
        )

        Text(
            text = "${(fill * 100f).roundToInt()}%",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            ),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = percentAlpha }
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (insetPx + offset).roundToInt(),
                        y = insetPx.roundToInt()
                    )
                }
                .size(68.dp)
                .graphicsLayer {
                    alpha = slideAlpha
                    scaleX = 0.72f + 0.28f * slideAlpha
                    scaleY = 0.72f + 0.28f * slideAlpha
                }
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Green.copy(alpha = 0.40f),
                    spotColor = Green.copy(alpha = 0.40f)
                )
                .clip(CircleShape)
                .background(if (enabled) Green else Green.copy(alpha = 0.35f))
                .pointerInput(maxOffset, enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragValue = settle.value
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragValue = (dragValue + dragAmount).coerceIn(0f, maxOffset)
                        },
                        onDragEnd = {
                            dragging = false
                            if (dragValue >= maxOffset * 0.8f) {
                                scope.launch {
                                    settle.snapTo(dragValue)
                                    settle.animateTo(
                                        targetValue = maxOffset,
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                                        )
                                    )
                                    onTrigger()
                                }
                            } else {
                                scope.launch {
                                    settle.snapTo(dragValue)
                                    settle.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                                        )
                                    )
                                    dragValue = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch {
                                settle.snapTo(dragValue)
                                settle.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                                    )
                                )
                                dragValue = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Slide to download",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        SuccessOverlay(
            visible = complete,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * Small analyzing glyph under the loader. The image icon is wiped by a bright
 * pass while a slim scan bar sweeps across it, then both mirror back and pause
 * for 200 ms, matching the analyzer reference animation.
 */
@Composable
private fun AnalyzingGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    val phase = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val ease = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1f)
        while (true) {
            phase.animateTo(1f, tween(1500, easing = ease))
            phase.animateTo(0f, tween(1000, easing = ease))
            delay(200)
        }
    }

    Canvas(modifier) {
        val u = size.minDimension / 24f
        drawAnalyzingGlyph(color.copy(alpha = 0.22f), u)

        clipRect(right = size.width * phase.value) {
            drawAnalyzingGlyph(color.copy(alpha = 0.92f), u)
        }

        val barWidth = size.width * 0.07f
        val barX = barWidth * (14f - 14.8f * phase.value)
        drawRoundRect(
            color = color.copy(alpha = 0.92f),
            topLeft = Offset(barX, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = CornerRadius(barWidth / 2f)
        )
    }
}

private fun DrawScope.drawAnalyzingGlyph(color: Color, u: Float) {
    val stroke = 1.5f * u

    drawRoundRect(
        color = color,
        topLeft = Offset(2f * u, 3f * u),
        size = Size(20f * u, 18f * u),
        cornerRadius = CornerRadius(2.2f * u, 2.2f * u),
        style = Stroke(stroke)
    )

    val peaks = Path().apply {
        moveTo(4.27f * u, 20.73f * u)
        lineTo(10.87f * u, 14.13f * u)
        quadraticBezierTo(12f * u, 13.05f * u, 13.13f * u, 14.13f * u)
        lineTo(19.68f * u, 20.68f * u)
        moveTo(14f * u, 15f * u)
        lineTo(16.87f * u, 12.13f * u)
        quadraticBezierTo(18f * u, 11.05f * u, 19.13f * u, 12.13f * u)
        lineTo(22f * u, 15f * u)
    }
    drawPath(
        path = peaks,
        color = color,
        style = Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    drawCircle(
        color = color,
        radius = 2f * u,
        center = Offset(8f * u, 9f * u),
        style = Stroke(stroke)
    )
}

private data class Sparkle(
    val angle: Float,
    val distance: Float,
    val color: Color
)

@Composable
private fun SuccessOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val circle = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    val sparkles = remember {
        val colors = listOf(
            Color.White,
            Green,
            Color(0xFFA7F3C5),
            Color(0xFFFFD700)
        )
        List(16) { index ->
            Sparkle(
                angle = (index / 16f) * (2f * PI.toFloat()),
                distance = 36f + Random.nextFloat() * 28f,
                color = colors[index % colors.size]
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            scale.snapTo(0f)
            alpha.snapTo(0f)
            circle.snapTo(0f)
            check.snapTo(0f)
            burst.snapTo(0f)
            // Let the green bar finish filling before the tick pops.
            delay(180)
            launch { alpha.animateTo(1f, tween(300)) }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 760,
                        easing = CubicBezierEasing(0.34f, 1.42f, 0.64f, 1f)
                    )
                )
            }
            launch {
                delay(120)
                circle.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 900,
                        easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                    )
                )
            }
            launch {
                delay(850)
                check.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 700,
                        easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                    )
                )
            }
            launch {
                delay(650)
                burst.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1500,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                )
            }
        } else {
            alpha.animateTo(0f, tween(250))
            scale.snapTo(0f)
        }
    }

    if (!visible && alpha.value <= 0.001f) return

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(132.dp)) {
            val radius = size.minDimension * (0.22f + 0.24f * burst.value)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Green.copy(alpha = 0.34f * alpha.value * (1f - burst.value * 0.6f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        sparkles.forEach { sparkle ->
            val dx = cos(sparkle.angle) * sparkle.distance * burst.value
            val dy = sin(sparkle.angle) * sparkle.distance * burst.value
            Canvas(
                modifier = Modifier
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(9.dp)
                    .graphicsLayer {
                        val remaining = 1f - burst.value
                        scaleX = remaining
                        scaleY = remaining
                        rotationZ = 360f * burst.value
                        this.alpha = remaining
                    }
            ) {
                // Four-point star spark with a soft round core.
                val stroke = size.minDimension * 0.16f
                val c = center
                drawLine(
                    sparkle.color,
                    Offset(c.x, 0f),
                    Offset(c.x, size.height),
                    stroke,
                    StrokeCap.Round
                )
                drawLine(
                    sparkle.color,
                    Offset(0f, c.y),
                    Offset(size.width, c.y),
                    stroke,
                    StrokeCap.Round
                )
                drawCircle(sparkle.color, radius = size.minDimension * 0.16f, center = c)
            }
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .shadow(
                    elevation = 32.dp,
                    shape = CircleShape,
                    ambientColor = Green,
                    spotColor = Green
                )
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(32.dp)) {
                val s = size.width / 52f
                val center = Offset(26f * s, 26f * s)

                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 360f * circle.value,
                    useCenter = false,
                    topLeft = Offset(center.x - 25f * s, center.y - 25f * s),
                    size = Size(50f * s, 50f * s),
                    style = Stroke(width = 3f * s)
                )

                if (check.value > 0f) {
                    val path = Path().apply {
                        moveTo(14.1f * s, 27.2f * s)
                        lineTo(21.2f * s, 34.4f * s)
                        lineTo(37.9f * s, 17.6f * s)
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val segment = Path()
                    measure.getSegment(
                        startDistance = 0f,
                        stopDistance = measure.length * check.value,
                        destination = segment,
                        startWithMoveTo = true
                    )
                    drawPath(
                        path = segment,
                        color = Color.White,
                        style = Stroke(
                            width = 4f * s,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
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
