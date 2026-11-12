package com.vidvibe.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidvibe.app.data.VideoItem
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val PlayerGreen = Color(0xFF30D158)

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
    }
    var brightness by remember(activity) {
        mutableFloatStateOf(activity.currentWindowBrightness())
    }
    var volume by remember(audioManager) {
        mutableFloatStateOf(
            audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 0f
        )
    }
    var showGestureOverlay by remember { mutableStateOf(false) }
    var overlayIsBrightness by remember { mutableStateOf(true) }
    var overlayValue by remember { mutableFloatStateOf(0f) }
    var gestureTick by remember { mutableIntStateOf(0) }

    var ended by remember(video.uri) { mutableStateOf(false) }
    var isPlaying by remember(video.uri) { mutableStateOf(true) }
    var showPlayPause by remember { mutableStateOf(false) }
    var playPauseTick by remember { mutableIntStateOf(0) }
    var seekHintSeconds by remember { mutableStateOf<Int?>(null) }
    // Wide screens start zoomed so the video fills the display instead of
    // sitting inside black bars. Phones keep the whole frame visible.
    val configuration = LocalConfiguration.current
    var fillScreen by rememberSaveable {
        mutableStateOf(configuration.screenWidthDp >= 600)
    }
    var seekTick by remember { mutableIntStateOf(0) }

    var resumePosition by remember { mutableLongStateOf(0L) }
    var inPip by remember { mutableStateOf(false) }

    fun showOverlay(isBrightness: Boolean, value: Float) {
        overlayIsBrightness = isBrightness
        overlayValue = value.coerceIn(0f, 1f)
        showGestureOverlay = true
        gestureTick++
    }

    LaunchedEffect(gestureTick) {
        if (showGestureOverlay) {
            delay(950)
            showGestureOverlay = false
        }
    }

    LaunchedEffect(seekTick) {
        if (seekHintSeconds != null) {
            delay(720)
            seekHintSeconds = null
        }
    }

    val exoPlayer = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            if (resumePosition > 0L) seekTo(resumePosition)
            playWhenReady = true
        }
    }

    fun seekBy(seconds: Int) {
        val duration = exoPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        val target = (exoPlayer.currentPosition + seconds * 1000L)
            .coerceIn(0L, duration)
        exoPlayer.seekTo(target)
        seekHintSeconds = seconds
        seekTick++
    }

    fun replay() {
        exoPlayer.seekTo(0L)
        exoPlayer.play()
        ended = false
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (ended) {
                exoPlayer.seekTo(0L)
                ended = false
            }
            exoPlayer.play()
        }
        showPlayPause = true
        playPauseTick++
    }

    LaunchedEffect(playPauseTick) {
        if (showPlayPause) {
            delay(700)
            showPlayPause = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    ended = true
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            resumePosition = exoPlayer.currentPosition
            delay(400)
        }
    }

    LaunchedEffect(activity) {
        while (true) {
            inPip = activity?.isInPictureInPictureMode == true
            delay(300)
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(view, inPip) {
        view.keepScreenOn = !inPip
        onDispose { view.keepScreenOn = false }
    }

    BackHandler {
        currentOnBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { playerContext ->
                PlayerView(playerContext).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setKeepContentOnPlayerReset(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.useController = !inPip
                view.resizeMode = if (fillScreen) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Left third: swipe up/down for brightness, double-tap to rewind.
        // Right third: swipe up/down for volume, double-tap to fast-forward.
        // Horizontal swipes on either side move through the queue.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.32f)
                .pointerInput(hasNext, hasPrevious) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            if (dragTotal >= 90f && hasNext) currentOnNext()
                            if (dragTotal <= -90f && hasPrevious) currentOnPrevious()
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
                .pointerInput(exoPlayer) {
                    detectTapGestures(onDoubleTap = { seekBy(-10) })
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val fraction = dragAmount / size.height.toFloat()
                            brightness = (brightness - fraction).coerceIn(0.01f, 1f)
                            activity.applyWindowBrightness(brightness)
                            showOverlay(true, brightness)
                        }
                    )
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.32f)
                .pointerInput(hasNext, hasPrevious) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            if (dragTotal >= 90f && hasNext) currentOnNext()
                            if (dragTotal <= -90f && hasPrevious) currentOnPrevious()
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
                .pointerInput(exoPlayer) {
                    detectTapGestures(onDoubleTap = { seekBy(10) })
                }
                .pointerInput(maxVolume) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val fraction = dragAmount / size.height.toFloat()
                            volume = (volume - fraction * maxVolume * 2.4f)
                                .coerceIn(0f, maxVolume.toFloat())
                            audioManager?.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                volume.roundToInt(),
                                0
                            )
                            showOverlay(false, volume / maxVolume.toFloat())
                        }
                    )
                }
        )

        // Centre third: tap to pause or play, horizontal swipe for the queue.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.36f)
                .fillMaxHeight(0.62f)
                .pointerInput(hasNext, hasPrevious) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            if (dragTotal >= 90f && hasNext) currentOnNext()
                            if (dragTotal <= -90f && hasPrevious) currentOnPrevious()
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { togglePlayPause() })
                }
        )

        AnimatedVisibility(
            visible = showPlayPause,
            enter = fadeIn(tween(110)) + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 620f)
            ),
            exit = fadeOut(tween(220)) + scaleOut(
                targetScale = 1.22f,
                animationSpec = tween(220)
            ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = PlayerGreen,
                        spotColor = PlayerGreen.copy(alpha = 0.45f)
                    )
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Outlined.Pause
                    } else {
                        Icons.Outlined.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showGestureOverlay,
            enter = fadeIn(tween(140)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
            ),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureIndicator(
                isBrightness = overlayIsBrightness,
                value = overlayValue
            )
        }

        AnimatedVisibility(
            visible = seekHintSeconds != null,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekIndicator(seconds = seekHintSeconds ?: 0)
        }

        AnimatedVisibility(
            visible = ended,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .pointerInput(ended, hasNext, hasPrevious) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragTotal += dragAmount
                            },
                            onDragEnd = {
                                if (dragTotal >= 90f && hasNext) currentOnNext()
                                if (dragTotal <= -90f && hasPrevious) currentOnPrevious()
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                PlayerEndedCard(
                    title = video.displayName,
                    hasNext = hasNext,
                    hasPrevious = hasPrevious,
                    onPrevious = { currentOnPrevious() },
                    onNext = { currentOnNext() },
                    onReplay = { replay() }
                )
            }
        }

        if (!inPip) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .clickable(onClick = { currentOnBack() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = video.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.durationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.Black.copy(alpha = if (fillScreen) 0.55f else 0.35f),
                                CircleShape
                            )
                            .clickable { fillScreen = !fillScreen },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (fillScreen) {
                                Icons.Outlined.FitScreen
                            } else {
                                Icons.Outlined.AspectRatio
                            },
                            contentDescription = if (fillScreen) {
                                "Fit the whole video"
                            } else {
                                "Fill the screen"
                            },
                            tint = if (fillScreen) PlayerGreen else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .clickable { shareVideo(context, video) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }

    }
}

@Composable
private fun PlayerEndedCard(
    title: String,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplay: () -> Unit
) {
    val cardShape = RoundedCornerShape(32.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .shadow(
                elevation = 28.dp,
                shape = cardShape,
                ambientColor = PlayerGreen,
                spotColor = PlayerGreen.copy(alpha = 0.30f)
            )
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2B2B33), Color(0xFF15151A))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), cardShape)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = PlayerGreen,
                    spotColor = PlayerGreen
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF43E884), Color(0xFF1C9E49))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Finished",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.58f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                hasNext && hasPrevious -> "Swipe right for the next video, left to go back"
                hasNext -> "Swipe right for the next video"
                hasPrevious -> "Swipe left to go back"
                else -> "That was the last video in this list"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.60f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerClayPill(
                icon = Icons.Outlined.SkipPrevious,
                label = "Previous",
                enabled = hasPrevious,
                onClick = onPrevious
            )
            PlayerClayPill(
                icon = if (hasNext) Icons.Outlined.SkipNext else Icons.Outlined.Replay,
                label = if (hasNext) "Next" else "Replay",
                enabled = true,
                primary = true,
                onClick = { if (hasNext) onNext() else onReplay() }
            )
        }
    }
}

@Composable
private fun PlayerClayPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .height(46.dp)
            .shadow(
                elevation = if (enabled && primary) 12.dp else 0.dp,
                shape = shape,
                ambientColor = PlayerGreen,
                spotColor = PlayerGreen.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(
                when {
                    !enabled -> Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))
                    )
                    primary -> Brush.verticalGradient(
                        listOf(Color(0xFF3BDB78), Color(0xFF1F9E4A))
                    )
                    else -> Brush.verticalGradient(
                        listOf(Color(0xFF3A3A44), Color(0xFF23232B))
                    )
                }
            )
            .border(
                1.dp,
                if (primary) Color.White.copy(alpha = 0.28f)
                else Color.White.copy(alpha = 0.12f),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.30f)
                    else -> Color.White
                },
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !enabled -> Color.White.copy(alpha = 0.30f)
                    else -> Color.White
                }
            )
        }
    }
}

@Composable
private fun SeekIndicator(seconds: Int) {
    val forward = seconds >= 0
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(22.dp))
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (forward) {
                    Icons.Outlined.FastForward
                } else {
                    Icons.Outlined.FastRewind
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${if (forward) "+" else ""}${seconds}s",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GestureIndicator(
    isBrightness: Boolean,
    value: Float
) {
    val percentage = (value * 100f).roundToInt()
    Box(
        modifier = Modifier
            .width(190.dp)
            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isBrightness) {
                        Icons.Outlined.BrightnessHigh
                    } else {
                        Icons.Outlined.VolumeUp
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isBrightness) "Brightness" else "Volume",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(value)
                        .fillMaxHeight()
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

private fun shareVideo(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, video.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri("video", video.uri)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share video"))
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Activity?.currentWindowBrightness(): Float {
    val value = this?.window?.attributes?.screenBrightness ?: -1f
    return if (value < 0f) 0.5f else value
}

private fun Activity?.applyWindowBrightness(value: Float) {
    val window = this?.window ?: return
    val attributes = window.attributes
    attributes.screenBrightness = value.coerceIn(0.01f, 1f)
    window.attributes = attributes
}
