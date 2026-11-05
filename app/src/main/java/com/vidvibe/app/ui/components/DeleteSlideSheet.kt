package com.vidvibe.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeGreen
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeSurface4
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DeletePhase {
    Ready,
    Deleting,
    Success,
    Error
}

@Composable
fun DeleteSlideSheet(
    visible: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: suspend () -> Boolean,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    slideLabel: String? = null
) {
    BackHandler(enabled = visible) {
        if (!visible) return@BackHandler
        onDismiss()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScopedAnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        ScopedAnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f)
            ) + fadeIn(tween(160)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DeleteSheetContent(
                visible = visible,
                selectedCount = selectedCount,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                onConfirmed = onConfirmed,
                title = title,
                description = description,
                slideLabel = slideLabel
            )
        }
    }
}

@Composable
private fun DeleteSheetContent(
    visible: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: suspend () -> Boolean,
    onConfirmed: () -> Unit,
    title: String?,
    description: String?,
    slideLabel: String?
) {
    var phase by remember { mutableStateOf(DeletePhase.Ready) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val thumbSize = 48.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackPaddingPx = with(density) { 4.dp.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val maxOffsetPx = (trackWidthPx - thumbSizePx - trackPaddingPx * 2f)
        .coerceAtLeast(1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val thumbOffset = remember { Animatable(0f) }
    val offset = if (isDragging) dragOffset else thumbOffset.value
    val progress = if (trackWidthPx <= 0) 0f else {
        ((offset + thumbSizePx / 2f) / trackWidthPx.toFloat()).coerceIn(0f, 1f)
    }

    val bounceTransition = rememberInfiniteTransition(label = "trash-bounce")
    val bouncePhase by bounceTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trash-bounce-phase"
    )
    val bounce = sin(bouncePhase * 2f * PI.toFloat())
    val trashTranslateY = if (phase == DeletePhase.Ready) bounce * -10f else 0f
    val trashRotation = if (phase == DeletePhase.Ready) bounce * -6f else 0f

    val particleProgress = remember { Animatable(0f) }
    val particleValue = particleProgress.value
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "thumb-scale"
    )

    fun resetTrack() {
        scope.launch {
            isDragging = false
            dragOffset = 0f
            thumbOffset.snapTo(0f)
        }
    }

    fun confirmDelete() {
        if (phase != DeletePhase.Ready) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            isDragging = false
            phase = DeletePhase.Deleting
            thumbOffset.animateTo(maxOffsetPx, tween(220, easing = FastOutSlowInEasing))
            val succeeded = runCatching { onConfirm() }.getOrDefault(false)
            if (succeeded) {
                phase = DeletePhase.Success
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                particleProgress.snapTo(0f)
                particleProgress.animateTo(1f, tween(620, easing = LinearEasing))
                kotlinx.coroutines.delay(1050)
                onConfirmed()
            } else {
                phase = DeletePhase.Error
                kotlinx.coroutines.delay(1200)
                phase = DeletePhase.Ready
                resetTrack()
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            phase = DeletePhase.Ready
            isDragging = false
            dragOffset = 0f
            thumbOffset.snapTo(0f)
            particleProgress.snapTo(0f)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1B24),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (phase == DeletePhase.Success) {
                    ParticleBurst(progress = particleValue)
                }

                ScopedAnimatedVisibility(
                    visible = phase != DeletePhase.Success && phase != DeletePhase.Error,
                    enter = fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
                    ),
                    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                translationY = trashTranslateY
                                rotationZ = trashRotation
                                scaleX = 1f + (bounce * 0.06f)
                                scaleY = 1f + (bounce * 0.06f)
                            }
                    )
                }

                ScopedAnimatedVisibility(
                    visible = phase == DeletePhase.Success,
                    enter = scaleIn(
                        initialScale = 0f,
                        animationSpec = spring(dampingRatio = 0.42f, stiffness = 380f)
                    ) + fadeIn(tween(220)),
                    exit = scaleOut(targetScale = 0f) + fadeOut(tween(120))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Done,
                        contentDescription = null,
                        tint = VidVibeGreen,
                        modifier = Modifier.size(56.dp)
                    )
                }

                ScopedAnimatedVisibility(
                    visible = phase == DeletePhase.Error,
                    enter = scaleIn(
                        initialScale = 0f,
                        animationSpec = spring(dampingRatio = 0.42f, stiffness = 380f)
                    ) + fadeIn(tween(220)),
                    exit = scaleOut(targetScale = 0f) + fadeOut(tween(120))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = VidVibeRed,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = title ?: "Delete $selectedCount ${if (selectedCount == 1) "video" else "videos"}?",
                style = MaterialTheme.typography.titleMedium,
                color = VidVibeText,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description ?: "These files will be permanently deleted from this device. This cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = VidVibeTextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(VidVibeSurface4)
                    .border(1.dp, VidVibeBorder, RoundedCornerShape(28.dp))
                    .onSizeChanged { trackWidthPx = it.width }
                    .pointerInput(maxOffsetPx, phase) {
                        if (phase != DeletePhase.Ready) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffset = thumbOffset.value
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, maxOffsetPx)
                            },
                            onDragEnd = {
                                isDragging = false
                                if (dragOffset >= maxOffsetPx * 0.9f) {
                                    val releaseOffset = dragOffset
                                    scope.launch { thumbOffset.snapTo(releaseOffset) }
                                    confirmDelete()
                                } else {
                                    scope.launch {
                                        thumbOffset.snapTo(dragOffset)
                                        thumbOffset.animateTo(
                                            0f,
                                            tween(220, easing = FastOutSlowInEasing)
                                        )
                                        dragOffset = 0f
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                scope.launch {
                                    thumbOffset.snapTo(dragOffset)
                                    thumbOffset.animateTo(
                                        0f,
                                        tween(220, easing = FastOutSlowInEasing)
                                    )
                                    dragOffset = 0f
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(56.dp)
                        .background(VidVibeGreen)
                )

                Text(
                    text = slideLabel ?: "Slide to Delete $selectedCount ${if (selectedCount == 1) "file" else "files"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = VidVibeTextSecondary.copy(
                        alpha = (1f - progress * 0.9f).coerceIn(0.1f, 1f)
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )

                ScopedAnimatedVisibility(
                    visible = phase == DeletePhase.Deleting,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    SpinnerBadge(size = 36.dp, bladeSize = 18.dp)
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (offset + trackPaddingPx).roundToInt(),
                                y = with(density) { 4.dp.roundToPx() }
                            )
                        }
                        .size(thumbSize)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .clip(CircleShape)
                        .background(
                            when (phase) {
                                DeletePhase.Success -> VidVibeGreen
                                DeletePhase.Error -> VidVibeRed
                                else -> VidVibeSurface3
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (phase == DeletePhase.Ready || phase == DeletePhase.Error) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Slide to delete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Permanent deletion",
                    style = MaterialTheme.typography.labelSmall,
                    color = VidVibeTextTertiary
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(VidVibeRed, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "This device",
                    style = MaterialTheme.typography.labelSmall,
                    color = VidVibeTextTertiary
                )
            }
        }
    }
}

@Composable
private fun ParticleBurst(progress: Float) {
    Canvas(modifier = Modifier.size(120.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val value = progress
        if (value <= 0f) return@Canvas
        val alpha = (1f - value).coerceIn(0f, 1f)
        val radius = 22.dp.toPx() + value * 32.dp.toPx()
        for (index in 0 until 12) {
            val angle = (index * 30f) * (PI.toFloat() / 180f)
            val dotRadius = if (index % 3 == 0) 2.4.dp.toPx() else 1.5.dp.toPx()
            drawCircle(
                color = VidVibeGreen.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(
                    x = center.x + cos(angle) * radius,
                    y = center.y + sin(angle) * radius
                )
            )
        }
        drawCircle(
            color = VidVibeGreen.copy(alpha = alpha * 0.5f),
            radius = 28.dp.toPx() + value * 22.dp.toPx(),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
