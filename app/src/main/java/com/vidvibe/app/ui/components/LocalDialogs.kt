package com.vidvibe.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vidvibe.app.data.Playlist
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.VidVibeAccentRed
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class PlaylistCreatePhase {
    Name,
    Creating
}

@Composable
fun PlaylistNameDialog(
    visible: Boolean,
    title: String = "New Playlist",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!visible) return

    var name by remember(visible) { mutableStateOf("") }
    var phase by remember(visible) { mutableStateOf(PlaylistCreatePhase.Name) }
    var showError by remember(visible) { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableIntStateOf(0) }

    val slide = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val textColor = LocalAppTextColor.current
    val creating = phase == PlaylistCreatePhase.Creating

    val handlePx = with(density) { 48.dp.toPx() }
    val insetPx = with(density) { 4.dp.toPx() }
    val maxOffset = (trackWidthPx - handlePx - insetPx * 2f).coerceAtLeast(1f)
    val current = if (dragging) dragValue else slide.value

    val shimmer by rememberInfiniteTransition(label = "create-shimmer").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "create-shimmer-value"
    )

    LaunchedEffect(visible) {
        delay(140)
        focusRequester.requestFocus()
    }

    LaunchedEffect(creating) {
        if (creating) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
            )
            delay(240)
            onSave(name.trim())
        }
    }

    fun snapBack() {
        scope.launch {
            slide.snapTo(dragValue)
            slide.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                )
            )
            dragValue = 0f
        }
    }

    fun beginCreating() {
        if (name.isBlank()) {
            showError = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            snapBack()
            return
        }
        showError = false
        keyboard?.hide()
        phase = PlaylistCreatePhase.Creating
    }

    Dialog(
        onDismissRequest = { if (!creating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF21C1C24),
                                Color(0xF20E0E13)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(22.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = textColor
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (creating) {
                        "Setting up your playlist..."
                    } else {
                        "Give your playlist a name, then swipe to create it."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFFA0A0A8)
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) showError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = !creating,
                    placeholder = {
                        Text(
                            "Playlist name",
                            color = Color(0xFF6B6B75),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboard?.hide() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White.copy(alpha = 0.28f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        disabledBorderColor = Color.White.copy(alpha = 0.10f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor.copy(alpha = 0.55f),
                        cursorColor = Color(0xFF30D158)
                    )
                )

                if (showError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enter a playlist name",
                        fontSize = 12.sp,
                        color = Color(0xFFFF453A)
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (creating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.value)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF1FA84A),
                                            Color(0xFF30D158)
                                        )
                                    )
                                )
                        )
                        Text(
                            text = "${(progress.value * 100).roundToInt()}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0x1A30D158))
                            .border(
                                width = 1.dp,
                                color = Color(0x4D30D158),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .onSizeChanged { trackWidthPx = it.width }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Swipe to Create",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = Color(0xFF30D158).copy(alpha = shimmer)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset { IntOffset(current.roundToInt(), 0) }
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF30D158),
                                            Color(0xFF1FA84A)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                                .pointerInput(maxOffset) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            dragging = true
                                            dragValue = slide.value
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragValue = (dragValue + dragAmount)
                                                .coerceIn(0f, maxOffset)
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            if (dragValue >= maxOffset * 0.8f) {
                                                scope.launch {
                                                    slide.snapTo(dragValue)
                                                    slide.animateTo(
                                                        targetValue = maxOffset,
                                                        animationSpec = tween(
                                                            durationMillis = 180,
                                                            easing = CubicBezierCubicEasing()
                                                        )
                                                    )
                                                    beginCreating()
                                                }
                                            } else {
                                                snapBack()
                                            }
                                        },
                                        onDragCancel = {
                                            dragging = false
                                            snapBack()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Swipe to create playlist",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (!creating) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA0A0A8),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { keyboard?.hide(); onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun CubicBezierCubicEasing(): Easing =
    CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

@Composable
fun PlaylistPickerDialog(
    visible: Boolean,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                color = Color(0xFF1E1F28),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                shadowElevation = 24.dp
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        text = "Add to playlist",
                        style = MaterialTheme.typography.titleMedium,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists yet. Create one to group these videos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VidVibeTextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            playlists.forEach { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(playlist) }
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlaylistPlay,
                                        contentDescription = null,
                                        tint = VidVibeTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = VidVibeText
                                        )
                                        Text(
                                            text = "${playlist.videoIds.size} videos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VidVibeTextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCreateNew() }
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = VidVibeAccentRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "New playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VidVibeAccentRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = VidVibeTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                if (enabled) container else container.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) content else content.copy(alpha = 0.45f)
        )
    }
}
