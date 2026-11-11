package com.vidvibe.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidvibe.app.data.TrimExporter
import com.vidvibe.app.data.TrimResult
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private val TrimGreen = Color(0xFF30D158)
private val TrimBlue = Color(0xFF4E9BFF)
private val TrimPanel = Color(0xF00E0E13)

/**
 * Lossless trim editor. The clip is previewed with a real ExoPlayer so the
 * handles line up with what will be exported, then [TrimExporter] stream-copies
 * the chosen range into the gallery without touching the original.
 */
@Composable
fun TrimEditorScreen(
    video: VideoItem,
    onClose: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var durationMs by remember(video.uri) {
        mutableLongStateOf(video.durationMs.coerceAtLeast(1L))
    }
    var startMs by remember(video.uri) { mutableLongStateOf(0L) }
    var endMs by remember(video.uri) {
        mutableLongStateOf(video.durationMs.coerceAtLeast(1L))
    }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedName by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    // ExoPlayer reports positions relative to a clipped item, so the start of
    // the last applied clip is kept here to map the playhead back to the real
    // timeline. Without it the preview bar drifts after every clip change.
    var clipStartMs by remember { mutableLongStateOf(0L) }

    val player = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
        }
    }

    fun applyClip(play: Boolean) {
        clipStartMs = startMs
        val clip = MediaItem.Builder()
            .setUri(video.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        player.setMediaItem(clip)
        player.prepare()
        if (play) player.play()
        playing = play
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.duration > 0L) {
                    val total = player.duration
                    if (total > 1L && durationMs < total - 500L) {
                        durationMs = total
                        endMs = total
                    }
                }
                if (playbackState == Player.STATE_ENDED) playing = false
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, playing, clipStartMs) {
        while (playing) {
            positionMs = (clipStartMs + player.currentPosition)
                .coerceIn(startMs, endMs)
            delay(220)
        }
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = !exporting) { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08080B), Color(0xFF0C0C11))
                )
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1.4f)) {
                        TrimHeader(onClose = onClose, enabled = !exporting)
                        Spacer(Modifier.height(10.dp))
                        TrimPreview(
                            player = player,
                            playing = playing,
                            positionMs = positionMs,
                            startMs = startMs,
                            endMs = endMs,
                            onToggle = {
                                if (playing) {
                                    player.pause()
                                } else {
                                    if (positionMs < startMs || positionMs >= endMs) {
                                        applyClip(true)
                                    } else {
                                        player.play()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .width(360.dp)
                            .fillMaxHeight()
                    ) {
                        TrimControls(
                            durationMs = durationMs,
                            startMs = startMs,
                            endMs = endMs,
                            positionMs = positionMs,
                            exporting = exporting,
                            progress = progress,
                            error = error,
                            onRangeChange = { range ->
                                startMs = (range.start * 1000f).roundToLong()
                                endMs = (range.endInclusive * 1000f).roundToLong()
                                error = null
                            },
                            onSetStart = {
                                startMs = positionMs.coerceIn(
                                    0L,
                                    (endMs - 500L).coerceAtLeast(0L)
                                )
                                error = null
                            },
                            onSetEnd = {
                                endMs = positionMs.coerceIn(
                                    startMs + 500L,
                                    durationMs
                                )
                                error = null
                            },
                            onPreview = { applyClip(true) },
                            onExport = {
                                exporting = true
                                progress = 0f
                                error = null
                                scope.launch {
                                    when (
                                        val result = TrimExporter(context).export(
                                            source = video.uri,
                                            startMs = startMs,
                                            endMs = endMs,
                                            displayName = video.displayName
                                        ) { value -> progress = value }
                                    ) {
                                        is TrimResult.Success -> {
                                            exporting = false
                                            savedName = result.displayName
                                            onSaved(result.displayName)
                                        }

                                        is TrimResult.Failure -> {
                                            exporting = false
                                            error = result.message
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    TrimHeader(onClose = onClose, enabled = !exporting)
                    Spacer(Modifier.height(10.dp))
                    TrimPreview(
                        player = player,
                        playing = playing,
                        positionMs = positionMs,
                        startMs = startMs,
                        endMs = endMs,
                        onToggle = {
                            if (playing) {
                                player.pause()
                            } else {
                                if (positionMs < startMs || positionMs >= endMs) {
                                    applyClip(true)
                                } else {
                                    player.play()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(Modifier.height(12.dp))
                    TrimControls(
                        durationMs = durationMs,
                        startMs = startMs,
                        endMs = endMs,
                        positionMs = positionMs,
                        exporting = exporting,
                        progress = progress,
                        error = error,
                        onRangeChange = { range ->
                            startMs = (range.start * 1000f).roundToLong()
                            endMs = (range.endInclusive * 1000f).roundToLong()
                            error = null
                        },
                        onSetStart = {
                            startMs = positionMs.coerceIn(
                                0L,
                                (endMs - 500L).coerceAtLeast(0L)
                            )
                            error = null
                        },
                        onSetEnd = {
                            endMs = positionMs.coerceIn(
                                startMs + 500L,
                                durationMs
                            )
                            error = null
                        },
                        onPreview = { applyClip(true) },
                        onExport = {
                            exporting = true
                            progress = 0f
                            error = null
                            scope.launch {
                                when (
                                    val result = TrimExporter(context).export(
                                        source = video.uri,
                                        startMs = startMs,
                                        endMs = endMs,
                                        displayName = video.displayName
                                    ) { value -> progress = value }
                                ) {
                                    is TrimResult.Success -> {
                                        exporting = false
                                        savedName = result.displayName
                                        onSaved(result.displayName)
                                    }

                                    is TrimResult.Failure -> {
                                        exporting = false
                                        error = result.message
                                    }
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = exporting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TrimProgressOverlay(progress = progress)
        }

        AnimatedVisibility(
            visible = savedName != null,
            enter = fadeIn() + scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f)
            ),
            exit = fadeOut() + scaleOut(targetScale = 0.94f)
        ) {
            TrimSavedOverlay(
                name = savedName.orEmpty(),
                onDone = {
                    savedName = null
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun TrimHeader(onClose: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(enabled = enabled, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close trim editor",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Trim video",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Lossless cut, original quality kept",
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.46f)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(TrimBlue.copy(alpha = 0.16f))
                .border(1.dp, TrimBlue.copy(alpha = 0.36f), RoundedCornerShape(12.dp))
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = null,
                    tint = TrimBlue,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "No re-encode",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }
        }
    }
}

@Composable
private fun TrimPreview(
    player: ExoPlayer,
    playing: Boolean,
    positionMs: Long,
    startMs: Long,
    endMs: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (endMs > startMs) {
        ((positionMs - startMs).toFloat() / (endMs - startMs).toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .pointerInput(player) {
                    detectTapGestures(onTap = { onToggle() })
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize()
            )

            if (!playing) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play selection",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(positionMs),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = " / ${formatDuration(endMs)}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }

            if (playing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f))
                        .clickable { player.pause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(TrimGreen)
                )
            }
        }
    }
}

@Composable
private fun TrimControls(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    positionMs: Long,
    exporting: Boolean,
    progress: Float,
    error: String?,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onPreview: () -> Unit,
    onExport: () -> Unit
) {
    val durationSeconds = (durationMs / 1000f).coerceAtLeast(1f)
    val selected = (endMs - startMs).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TrimPanel)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimePill(label = "Start", value = formatDuration(startMs))
            Spacer(Modifier.width(8.dp))
            TimePill(label = "End", value = formatDuration(endMs))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CLIP",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.38f),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = formatDuration(selected),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrimGreen
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        RangeSlider(
            value = (startMs / 1000f)..(endMs / 1000f),
            onValueChange = onRangeChange,
            valueRange = 0f..durationSeconds,
            enabled = !exporting,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = TrimGreen,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GhostPill(
                text = "Start at playhead",
                enabled = !exporting,
                onClick = onSetStart,
                modifier = Modifier.weight(1f)
            )
            GhostPill(
                text = "End at playhead",
                enabled = !exporting,
                onClick = onSetEnd,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .clickable(enabled = !exporting, onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Preview",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (exporting) TrimGreen.copy(alpha = 0.35f) else TrimGreen
                    )
                    .clickable(enabled = !exporting, onClick = onExport),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (exporting) {
                        "Saving ${(progress * 100).toInt()}%"
                    } else {
                        "Save clip to gallery"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 10.5.sp,
                color = Color(0xFFFF8A8E),
                lineHeight = 14.sp
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The start snaps to the nearest keyframe. The original video stays untouched.",
                fontSize = 9.5.sp,
                color = Color.White.copy(alpha = 0.34f),
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun TimePill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.38f),
            letterSpacing = 0.7.sp
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun GhostPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = if (enabled) 0.74f else 0.32f)
        )
    }
}

@Composable
private fun TrimProgressOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(230.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14141A))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cutting clip",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Copying the selected range",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .background(TrimGreen, RoundedCornerShape(3.dp))
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TrimGreen
            )
        }
    }
}

@Composable
private fun TrimSavedOverlay(name: String, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(TrimGreen.copy(alpha = 0.16f))
                    .border(1.dp, TrimGreen.copy(alpha = 0.44f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = TrimGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Clip saved",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = name,
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.52f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TrimGreen)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
