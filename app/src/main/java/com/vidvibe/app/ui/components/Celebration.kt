package com.vidvibe.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.ui.theme.LocalGlassStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

val SuccessGreen = Color(0xFF30D158)

private data class StarSpark(
    val angle: Float,
    val distance: Float,
    val color: Color
)

/**
 * The slow-motion tick used for every save path: downloads, trimmed clips and
 * gallery writes. A ring draws itself, the check strokes in, then a soft glow
 * and sixteen four-point stars drift outward and fade. All timings are fixed
 * so the moment reads the same in every screen.
 */
@Composable
fun SuccessCelebration(
    visible: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = SuccessGreen,
    size: Dp = 160.dp
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val circle = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    val sparkles = remember(accent) {
        val colors = listOf(Color.White, accent, Color(0xFFA7F3C5), Color(0xFFFFD700))
        List(18) { index ->
            StarSpark(
                angle = (index / 18f) * (2f * PI.toFloat()) + Random.nextFloat() * 0.14f,
                distance = 38f + Random.nextFloat() * 30f,
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
            delay(140)
            launch { alpha.animateTo(1f, tween(300)) }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 780,
                        easing = CubicBezierEasing(0.34f, 1.42f, 0.64f, 1f)
                    )
                )
            }
            launch {
                delay(120)
                circle.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 950,
                        easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                    )
                )
            }
            launch {
                delay(880)
                check.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 760,
                        easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                    )
                )
            }
            launch {
                delay(620)
                burst.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1700,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                )
            }
        } else {
            alpha.animateTo(0f, tween(220))
            scale.snapTo(0f)
        }
    }

    if (!visible && alpha.value <= 0.001f) return

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.84f)) {
            val radius = this.size.minDimension * (0.22f + 0.26f * burst.value)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.32f * alpha.value * (1f - burst.value * 0.55f)),
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
                        rotationZ = 320f * burst.value
                        this.alpha = remaining
                    }
            ) {
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
                .size(size * 0.42f)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .shadow(
                    elevation = 30.dp,
                    shape = CircleShape,
                    ambientColor = accent,
                    spotColor = accent
                )
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(size * 0.22f)) {
                val s = this.size.width / 52f
                val c = Offset(26f * s, 26f * s)

                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 360f * circle.value,
                    useCenter = false,
                    topLeft = Offset(c.x - 25f * s, c.y - 25f * s),
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

/**
 * Slide-to-confirm control. The handle follows the finger, the track fills
 * behind it, and the action only fires past 90 percent, so a brush against the
 * screen can never save or delete anything.
 */
@Composable
fun SlideToConfirm(
    label: String,
    enabled: Boolean,
    accent: Color,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp
) {
    val glass = LocalGlassStyle.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var widthPx by remember { mutableIntStateOf(0) }
    var fraction by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val handlePx = 46f

    val shown by animateFloatAsState(
        targetValue = if (dragging) fraction else 0f,
        animationSpec = tween(240, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label = "slide-confirm"
    )

    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, glass.stroke, shape)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(enabled, widthPx) {
                if (!enabled || widthPx <= 0) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        fraction = 0f
                    },
                    onDragEnd = {
                        dragging = false
                        val fired = fraction >= 0.9f
                        if (fired) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm()
                        }
                        scope.launch { fraction = 0f }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { fraction = 0f }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val span = (widthPx - handlePx).coerceAtLeast(1f)
                    fraction = (fraction + dragAmount / span).coerceIn(0f, 1f)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(shown)
                .graphicsLayer { alpha = 0.18f + shown * 0.30f }
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(accent, accent.copy(alpha = 0.55f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 58.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.36f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .offset { IntOffset((shown * (widthPx - handlePx)).roundToInt(), 0) }
                .size(handlePx.dp)
                .clip(CircleShape)
                .background(accent)
                .shadow(12.dp, CircleShape, ambientColor = accent, spotColor = accent),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.82f),
                    modifier = Modifier.size(15.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.40f),
                    modifier = Modifier
                        .size(15.dp)
                        .offset(x = (-7).dp)
                )
            }
        }
    }
}

/**
 * Compact card shown when a background download lands in the gallery. It sits
 * above the navigation, never blocks input, and dismisses itself.
 */
@Composable
fun DownloadSavedCard(
    title: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassStyle.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(glass.bodyTop, glass.bodyBottom))
            )
            .border(1.dp, glass.stroke, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SuccessCelebration(visible = true, size = 92.dp)
        Spacer(Modifier.size(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Saved to your gallery",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.52f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, glass.stroke, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(onClick = onDone)
        ) {
            Text(
                text = "Done",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}
