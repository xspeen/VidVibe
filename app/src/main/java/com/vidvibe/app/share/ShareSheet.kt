package com.vidvibe.app.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.ui.viewmodel.DownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val qualities = listOf(
    2160 to "Best",
    1080 to "1080p",
    720 to "720p",
    480 to "480p",
    360 to "360p"
)

private val CardShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

@Composable
fun ShareSheet(
    initialLink: String,
    state: DownloadState,
    onDownload: (String, Int) -> Unit,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    var link by remember { mutableStateOf(initialLink) }
    var quality by remember { mutableIntStateOf(720) }
    var shown by remember { mutableStateOf(false) }

    val running = state is DownloadState.Running
    val complete = state is DownloadState.Complete

    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(complete) {
        if (complete) {
            delay(700)
            onSaved()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
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
                animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
            ) + fadeIn(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF2121216), Color(0xF8070709))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.26f), CardShape)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.30f))
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Save video",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    val host = remember(link) {
                        runCatching {
                            android.net.Uri.parse(link).host?.removePrefix("www.")
                        }.getOrNull().orEmpty()
                    }
                    if (host.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = host,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.12f),
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
                            fontSize = 11.sp
                        ),
                        cursorBrush = Brush.verticalGradient(
                            listOf(Color.White, Color.White)
                        ),
                        decorationBox = { inner ->
                            if (link.isBlank()) {
                                Text(
                                    text = "Paste a video link",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.38f)
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "QUALITY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    qualities.forEach { (height, label) ->
                        val selected = quality == height
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (selected) Color.White
                                    else Color.White.copy(alpha = 0.07f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) Color.White
                                    else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(9.dp)
                                )
                                .then(
                                    if (running) Modifier
                                    else Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { quality = height }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color(0xFF08080A)
                                else Color.White.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                            label = if (link.isBlank()) "Paste a link first" else "Slide to download",
                            enabled = link.isNotBlank(),
                            onTrigger = { onDownload(link.trim(), quality) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancel
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
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
    onTrigger: () -> Unit
) {
    val density = LocalDensity.current
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
            .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.04f))
            .border(
                1.dp,
                Color.White.copy(alpha = if (enabled) 0.28f else 0.10f),
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
                            Color.White.copy(alpha = 0.16f * fraction),
                            Color.White.copy(alpha = 0.06f * fraction)
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
