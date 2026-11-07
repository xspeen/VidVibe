package com.vidvibe.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val MenuBlue = Color(0xFF4E9BFF)
private val MenuRed = Color(0xFFFF453A)
private val MenuGreen = Color(0xFF30D158)
private val MenuText = Color(0xFFF5F5F7)
private val MenuText2 = Color(0xFFA0A0A8)
private val MenuText3 = Color(0xFF6B6B75)
private val MenuGlass = Color(0xCC14141A)
private val MenuGlassBottom = Color(0xB30A0A0C)

enum class FloatingPanel {
    Hidden,
    Actions,
    Quality,
    Slide,
    Progress
}

enum class FloatingMenuContext {
    Local,
    Trash,
    Downloader
}

enum class FloatingMenuColor {
    Delete,
    Restore,
    Download
}

class FloatingMenuState {
    var panel by mutableStateOf(FloatingPanel.Hidden)
        private set
    var context by mutableStateOf(FloatingMenuContext.Local)
        private set
    var color by mutableStateOf(FloatingMenuColor.Delete)
        private set
    var slideLabel by mutableStateOf("Slide to Confirm")
        private set
    var progress by mutableFloatStateOf(0f)
    var success by mutableStateOf(false)
    var autoProgress by mutableStateOf(false)
        private set

    fun openActions(context: FloatingMenuContext) {
        this.context = context
        this.color = if (context == FloatingMenuContext.Trash) {
            FloatingMenuColor.Restore
        } else {
            FloatingMenuColor.Delete
        }
        this.progress = 0f
        this.success = false
        this.autoProgress = false
        this.panel = FloatingPanel.Actions
    }

    fun openQuality() {
        this.progress = 0f
        this.success = false
        this.autoProgress = false
        this.panel = FloatingPanel.Quality
    }

    fun openSlide(
        color: FloatingMenuColor,
        label: String,
        autoProgress: Boolean = true
    ) {
        this.color = color
        this.slideLabel = label
        this.progress = 0f
        this.success = false
        this.autoProgress = autoProgress
        this.panel = FloatingPanel.Slide
    }

    fun beginProgress(auto: Boolean = false, progress: Float = 0f) {
        this.progress = progress.coerceIn(0f, 1f)
        this.success = false
        this.autoProgress = auto
        this.panel = FloatingPanel.Progress
    }

    fun setProgress(value: Float) {
        this.progress = value.coerceIn(0f, 1f)
    }

    fun showSuccess() {
        this.panel = FloatingPanel.Hidden
        this.autoProgress = false
        this.success = true
    }

    fun hide() {
        this.panel = FloatingPanel.Hidden
        this.success = false
        this.progress = 0f
        this.autoProgress = false
    }
}

@Composable
fun VidVibeFloatingMenu(
    state: FloatingMenuState,
    onShare: () -> Unit,
    onAddTo: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    onCancel: () -> Unit,
    onQualityPicked: (Int) -> Unit,
    onSlideConfirmed: () -> Unit,
    onAutoProgressFinished: () -> Unit,
    onSuccessFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val reveal = remember { Animatable(0f) }
    val menuVisible = state.panel != FloatingPanel.Hidden

    LaunchedEffect(menuVisible) {
        reveal.animateTo(
            targetValue = if (menuVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = 400,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
    }

    val quality = state.panel == FloatingPanel.Quality
    val menuHeight by animateDpAsState(
        targetValue = if (quality) 140.dp else 80.dp,
        animationSpec = tween(
            durationMillis = 350,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "floating-menu-height"
    )
    val menuRadius by animateDpAsState(
        targetValue = if (quality) 32.dp else 40.dp,
        animationSpec = tween(
            durationMillis = 400,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "floating-menu-radius"
    )

    var wrapHeightPx by remember { mutableIntStateOf(0) }
    val fallbackPx = with(density) { 300.dp.toPx() }
    val measuredPx = if (wrapHeightPx > 0) wrapHeightPx.toFloat() else fallbackPx
    val hiddenOffset = (1f - reveal.value) * measuredPx * 1.2f

    LaunchedEffect(state.panel, state.autoProgress) {
        if (state.panel == FloatingPanel.Progress && state.autoProgress) {
            var value = 0f
            while (value < 1f) {
                delay(50)
                value = (value + Random.nextInt(2, 7) / 100f).coerceAtMost(1f)
                state.progress = value
            }
            delay(600)
            onAutoProgressFinished()
        }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            delay(2500)
            onSuccessFinished()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { wrapHeightPx = it.height }
                .graphicsLayer { translationY = hiddenOffset }
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(menuHeight)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(menuRadius),
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
                    .clip(RoundedCornerShape(menuRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(MenuGlass, MenuGlassBottom)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(menuRadius)
                    )
            ) {
                when (state.panel) {
                    FloatingPanel.Actions -> ActionsPanel(
                        context = state.context,
                        onShare = onShare,
                        onAddTo = onAddTo,
                        onDelete = onDelete,
                        onRestore = onRestore,
                        onDeletePermanently = onDeletePermanently,
                        onCancel = onCancel
                    )

                    FloatingPanel.Quality -> QualityPanel(onQualityPicked)
                    FloatingPanel.Slide -> SlidePanel(
                        state = state,
                        onSlideConfirmed = onSlideConfirmed
                    )

                    FloatingPanel.Progress -> ProgressPanel(state)
                    FloatingPanel.Hidden -> Unit
                }
            }
        }

        SuccessOverlay(
            visible = state.success,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 74.dp)
        )
    }
}

@Composable
private fun ActionsPanel(
    context: FloatingMenuContext,
    onShare: () -> Unit,
    onAddTo: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (context == FloatingMenuContext.Local) {
            MenuAction(
                icon = Icons.Outlined.Share,
                label = "Share",
                tint = MenuBlue,
                background = MenuBlue.copy(alpha = 0.15f),
                onClick = onShare
            )
            MenuAction(
                icon = Icons.Outlined.PlaylistAdd,
                label = "Add to",
                tint = MenuText2,
                background = MenuText2.copy(alpha = 0.15f),
                onClick = onAddTo
            )
            MenuAction(
                icon = Icons.Outlined.DeleteOutline,
                label = "Delete",
                tint = MenuRed,
                background = MenuRed.copy(alpha = 0.15f),
                onClick = onDelete
            )
        } else {
            MenuAction(
                icon = Icons.Outlined.Restore,
                label = "Restore",
                tint = MenuGreen,
                background = MenuGreen.copy(alpha = 0.15f),
                onClick = onRestore
            )
            MenuAction(
                icon = Icons.Outlined.Close,
                label = "Delete",
                tint = MenuRed,
                background = MenuRed.copy(alpha = 0.15f),
                onClick = onDeletePermanently
            )
        }
        MenuAction(
            icon = Icons.Outlined.Close,
            label = "Cancel",
            tint = MenuText,
            background = Color.White.copy(alpha = 0.10f),
            onClick = onCancel
        )
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MenuText2,
            maxLines = 1
        )
    }
}

private data class QualityChoice(
    val height: Int,
    val title: String,
    val subtitle: String
)

private val qualityChoices = listOf(
    QualityChoice(2160, "4K", "2160p"),
    QualityChoice(1080, "1080p", "Full HD"),
    QualityChoice(720, "720p", "HD"),
    QualityChoice(480, "480p", "SD"),
    QualityChoice(360, "360p", "Low")
)

@Composable
private fun QualityPanel(onQualityPicked: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SELECT QUALITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MenuText2,
            letterSpacing = 0.96.sp,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            qualityChoices.forEach { choice ->
                QualityItem(
                    choice = choice,
                    onClick = { onQualityPicked(choice.height) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QualityItem(
    choice: QualityChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.04f else 1f,
        animationSpec = tween(150),
        label = "quality-scale"
    )

    Column(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (pressed) MenuBlue else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (pressed) MenuBlue else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = choice.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (pressed) Color.White else MenuText,
            maxLines = 1
        )
        Text(
            text = choice.subtitle,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (pressed) Color.White.copy(alpha = 0.80f) else MenuText3,
            maxLines = 1
        )
    }
}

@Composable
private fun SlidePanel(
    state: FloatingMenuState,
    onSlideConfirmed: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val insetPx = with(density) { 6.dp.toPx() }
    val handlePx = with(density) { 68.dp.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val maxOffset = (trackWidthPx - handlePx - insetPx * 2f).coerceAtLeast(1f)

    val settle = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val offset = if (dragging) dragValue else settle.value

    LaunchedEffect(state.panel) {
        if (state.panel == FloatingPanel.Slide) {
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
    val labelAlpha = pulseAlpha * dragFade

    val handleColor = when (state.color) {
        FloatingMenuColor.Delete -> MenuRed
        FloatingMenuColor.Restore, FloatingMenuColor.Download -> MenuGreen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { trackWidthPx = it.width }
    ) {
        Text(
            text = state.slideLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = labelAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
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
                .width(68.dp)
                .height(68.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = handleColor.copy(alpha = 0.40f),
                    spotColor = handleColor.copy(alpha = 0.40f)
                )
                .clip(CircleShape)
                .background(handleColor)
                .pointerInput(maxOffset, state.panel) {
                    if (state.panel != FloatingPanel.Slide) return@pointerInput
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
                                    onSlideConfirmed()
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
                contentDescription = "Slide to confirm",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ProgressPanel(state: FloatingMenuState) {
    val fill by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progress-fill"
    )
    val fillColor = when (state.color) {
        FloatingMenuColor.Delete -> MenuRed
        FloatingMenuColor.Restore, FloatingMenuColor.Download -> MenuGreen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(34.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .background(fillColor)
            )
        }
        Text(
            text = "${(state.progress * 100f).roundToInt()}%",
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
            modifier = Modifier.align(Alignment.Center)
        )
    }
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
        val colors = listOf(Color.White, MenuGreen, Color(0xFFA7F3C5))
        List(8) { index ->
            Sparkle(
                angle = (index / 8f) * (2f * PI.toFloat()),
                distance = 40f + Random.nextFloat() * 20f,
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
            launch { alpha.animateTo(1f, tween(250)) }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                    )
                )
            }
            launch {
                delay(100)
                circle.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                    )
                )
            }
            launch {
                delay(600)
                check.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                    )
                )
            }
            launch {
                delay(700)
                burst.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 800,
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
        sparkles.forEach { sparkle ->
            val dx = cos(sparkle.angle) * sparkle.distance * burst.value
            val dy = sin(sparkle.angle) * sparkle.distance * burst.value
            Box(
                modifier = Modifier
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(6.dp)
                    .graphicsLayer {
                        val remaining = 1f - burst.value
                        scaleX = remaining
                        scaleY = remaining
                        rotationZ = 360f * burst.value
                        this.alpha = remaining
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(sparkle.color)
            )
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
                    ambientColor = MenuGreen,
                    spotColor = MenuGreen
                )
                .clip(CircleShape)
                .background(MenuGreen),
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
