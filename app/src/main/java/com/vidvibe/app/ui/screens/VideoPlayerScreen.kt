package com.vidvibe.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.VideoRepository
import com.vidvibe.app.download.AudioPlaybackService
import com.vidvibe.app.download.PlaybackBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val PlayerGreen = Color(0xFF30D158)
private val PlayerBlue = Color(0xFF4E9BFF)

private data class PlayerSubtitleTrack(
    val label: String,
    val group: TrackGroup,
    val index: Int
)

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBack: () -> Unit,
    onTrim: (VideoItem) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { VideoRepository(context) }

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
    var seekHintSeconds by remember { mutableStateOf<Int?>(null) }
    // Wide screens start zoomed so the video fills the display instead of
    // sitting inside black bars. Phones keep the whole frame visible.
    val configuration = LocalConfiguration.current
    var fillScreen by rememberSaveable {
        mutableStateOf(configuration.screenWidthDp >= 600)
    }
    var seekTick by remember { mutableIntStateOf(0) }

    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var showTools by remember { mutableStateOf(false) }
    var backgroundAudio by remember { mutableStateOf(false) }
    var subtitleUri by remember { mutableStateOf<Uri?>(null) }
    var subtitleName by remember { mutableStateOf<String?>(null) }
    var resumePosition by remember { mutableLongStateOf(0L) }
    var inPip by remember { mutableStateOf(false) }
    var textTracks by remember { mutableStateOf<List<PlayerSubtitleTrack>>(emptyList()) }
    var selectedTrack by remember { mutableStateOf<String?>(null) }
    var captureMessage by remember { mutableStateOf<String?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(captureMessage) {
        if (captureMessage != null) {
            delay(2200)
            captureMessage = null
        }
    }

    val exoPlayer = remember(video.uri, subtitleUri) {
        val activeSubtitle = subtitleUri
        ExoPlayer.Builder(context).build().apply {
            val builder = MediaItem.Builder().setUri(video.uri)
            if (activeSubtitle != null) {
                val mime = subtitleMime(
                    subtitleName ?: activeSubtitle.lastPathSegment.orEmpty()
                )
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(activeSubtitle)
                            .setMimeType(mime)
                            .setLanguage("und")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                )
            }
            setMediaItem(builder.build())
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

    fun enterPip() {
        val target = activity ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            captureMessage = "Picture-in-picture needs Android 8 or newer"
            return
        }
        val width = video.width.takeIf { it > 0 } ?: 16
        val height = video.height.takeIf { it > 0 } ?: 9
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(width, height))
            .build()
        runCatching { target.enterPictureInPictureMode(params) }
            .onFailure { captureMessage = "Picture-in-picture is unavailable here" }
        showTools = false
    }

    fun captureFrame() {
        val positionUs = exoPlayer.currentPosition * 1000L
        flashTick++
        captureMessage = "Saving frame..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, video.uri)
                        val bitmap = retriever.getFrameAtTime(
                            positionUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        ) ?: retriever.frameAtTime
                            ?: error("No frame at this position")
                        val name = "VidVibe_frame_${System.currentTimeMillis()}.png"
                        repository.saveImageToGallery(bitmap, name)
                        name
                    } finally {
                        runCatching { retriever.release() }
                    }
                }
            }
            captureMessage = if (result.isSuccess) {
                "Frame saved to your gallery"
            } else {
                result.exceptionOrNull()?.message ?: "The frame could not be saved"
            }
        }
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            resumePosition = exoPlayer.currentPosition
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            subtitleName = displayNameOf(context, uri)
            subtitleUri = uri
            selectedTrack = null
            captureMessage = "Subtitle loaded"
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    ended = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(exoPlayer, speed) {
        exoPlayer.setPlaybackSpeed(speed)
        onDispose { }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            resumePosition = exoPlayer.currentPosition
            delay(400)
        }
    }

    LaunchedEffect(exoPlayer, showTools) {
        while (showTools) {
            textTracks = runCatching {
                exoPlayer.currentTracks.groups
                    .filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMap { group ->
                        (0 until group.length).mapNotNull { index ->
                            if (!group.isTrackSupported(index)) return@mapNotNull null
                            val format = group.getTrackFormat(index)
                            val label = format.label?.toString()
                                ?: format.language?.takeIf { it != "und" }
                                ?: "Track ${index + 1}"
                            PlayerSubtitleTrack(label, group.mediaTrackGroup, index)
                        }
                    }
            }.getOrDefault(emptyList())
            delay(700)
        }
    }

    LaunchedEffect(activity) {
        while (true) {
            inPip = activity?.isInPictureInPictureMode == true
            delay(300)
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, backgroundAudio) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !backgroundAudio) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer, backgroundAudio) {
        PlaybackBridge.onPause = { exoPlayer.pause() }
        PlaybackBridge.onStop = {
            exoPlayer.pause()
            AudioPlaybackService.stop(context)
        }
        if (backgroundAudio) {
            AudioPlaybackService.start(context, video.displayName)
        }
        onDispose {
            PlaybackBridge.onPause = null
            PlaybackBridge.onStop = null
            AudioPlaybackService.stop(context)
        }
    }

    DisposableEffect(view, inPip) {
        view.keepScreenOn = !inPip
        onDispose { view.keepScreenOn = false }
    }

    BackHandler {
        if (showTools) {
            showTools = false
        } else {
            currentOnBack()
        }
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
                            if (dragTotal <= -90f && hasNext) currentOnNext()
                            if (dragTotal >= 90f && hasPrevious) currentOnPrevious()
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
                            if (dragTotal <= -90f && hasNext) currentOnNext()
                            if (dragTotal >= 90f && hasPrevious) currentOnPrevious()
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
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .pointerInput(ended, hasNext, hasPrevious) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragTotal += dragAmount
                            },
                            onDragEnd = {
                                if (dragTotal <= -90f && hasNext) currentOnNext()
                                if (dragTotal >= 90f && hasPrevious) currentOnPrevious()
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 26.dp)
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Finished",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = video.displayName,
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
                            hasNext -> "Swipe left or tap Next for the next video"
                            hasPrevious -> "Swipe right or tap Previous to go back"
                            else -> "That was the last video in this list"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlayerActionPill(
                            icon = Icons.Outlined.SkipPrevious,
                            label = "Previous",
                            enabled = hasPrevious,
                            onClick = { currentOnPrevious() }
                        )
                        PlayerActionPill(
                            icon = if (hasNext) {
                                Icons.Outlined.SkipNext
                            } else {
                                Icons.Outlined.Replay
                            },
                            label = if (hasNext) "Next" else "Replay",
                            enabled = true,
                            primary = true,
                            onClick = {
                                if (hasNext) currentOnNext() else replay()
                            }
                        )
                    }
                }
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
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (speed != 1f) PlayerBlue.copy(alpha = 0.24f)
                                else Color.Black.copy(alpha = 0.35f)
                            )
                            .border(
                                1.dp,
                                if (speed != 1f) PlayerBlue.copy(alpha = 0.52f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(17.dp)
                            )
                            .clickable { showTools = true }
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatSpeed(speed),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (speed != 1f) PlayerBlue else Color.White
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.Black.copy(alpha = if (showTools) 0.60f else 0.35f),
                                CircleShape
                            )
                            .clickable { showTools = !showTools },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Player tools",
                            tint = if (showTools) PlayerGreen else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
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

        AnimatedVisibility(
            visible = flashTick > 0,
            enter = fadeIn(tween(60)),
            exit = fadeOut(tween(420)),
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.White.copy(alpha = 0.42f))
            }
        }
        LaunchedEffect(flashTick) {
            if (flashTick > 0) {
                delay(70)
                flashTick = 0
            }
        }

        AnimatedVisibility(
            visible = captureMessage != null,
            enter = fadeIn(tween(160)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = captureMessage.orEmpty(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        if (showTools) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.46f))
                    .clickable { showTools = false }
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val wide = maxWidth >= 600.dp && maxWidth > maxHeight
                AnimatedVisibility(
                    visible = showTools,
                    enter = if (wide) {
                        fadeIn(tween(160)) + scaleIn(initialScale = 0.96f)
                    } else {
                        slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(160))
                    },
                    exit = if (wide) {
                        fadeOut(tween(140))
                    } else {
                        slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(140))
                    },
                    modifier = if (wide) {
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                            .widthIn(max = 360.dp)
                            .width(340.dp)
                            .fillMaxHeight(0.92f)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .heightIn(max = 520.dp)
                    }
                ) {
                    PlayerToolsPanel(
                        speed = speed,
                        onSpeed = { speed = it },
                        backgroundAudio = backgroundAudio,
                        onToggleBackground = {
                            backgroundAudio = !backgroundAudio
                            if (!backgroundAudio) {
                                AudioPlaybackService.stop(context)
                            } else {
                                AudioPlaybackService.start(context, video.displayName)
                            }
                        },
                        subtitleName = subtitleName,
                        subtitleTracks = textTracks,
                        selectedTrack = selectedTrack,
                        onSelectTrack = { track ->
                            selectedTrack = track.label
                            val parameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(track.group, track.index)
                                )
                                .build()
                            exoPlayer.trackSelectionParameters = parameters
                        },
                        onSubtitlesOff = {
                            selectedTrack = null
                            exoPlayer.trackSelectionParameters =
                                exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .build()
                        },
                        onLoadSubtitle = { subtitlePicker.launch(arrayOf("*/*")) },
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        onPrevious = {
                            showTools = false
                            currentOnPrevious()
                        },
                        onNext = {
                            showTools = false
                            currentOnNext()
                        },
                        onCapture = {
                            showTools = false
                            captureFrame()
                        },
                        onPip = { enterPip() },
                        onTrim = {
                            showTools = false
                            exoPlayer.pause()
                            onTrim(video)
                        },
                        onClose = { showTools = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerToolsPanel(
    speed: Float,
    onSpeed: (Float) -> Unit,
    backgroundAudio: Boolean,
    onToggleBackground: () -> Unit,
    subtitleName: String?,
    subtitleTracks: List<PlayerSubtitleTrack>,
    selectedTrack: String?,
    onSelectTrack: (PlayerSubtitleTrack) -> Unit,
    onSubtitlesOff: () -> Unit,
    onLoadSubtitle: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCapture: () -> Unit,
    onPip: () -> Unit,
    onTrim: () -> Unit,
    onClose: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF21A1A21), Color(0xF20C0C11))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Player tools",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Speed, subtitles, capture and trim",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.44f)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close tools",
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolTile(
                icon = Icons.Outlined.Headphones,
                label = "Background audio",
                active = backgroundAudio,
                modifier = Modifier.weight(1f),
                onClick = onToggleBackground
            )
            ToolTile(
                icon = Icons.Outlined.PhotoCamera,
                label = "Capture frame",
                active = false,
                modifier = Modifier.weight(1f),
                onClick = onCapture
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolTile(
                icon = Icons.Outlined.PictureInPictureAlt,
                label = "Picture-in-picture",
                active = false,
                modifier = Modifier.weight(1f),
                onClick = onPip
            )
            ToolTile(
                icon = Icons.Outlined.ContentCut,
                label = "Trim video",
                active = false,
                modifier = Modifier.weight(1f),
                onClick = onTrim
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolTile(
                icon = Icons.Outlined.SkipPrevious,
                label = "Previous",
                active = false,
                enabled = hasPrevious,
                modifier = Modifier.weight(1f),
                onClick = onPrevious
            )
            ToolTile(
                icon = Icons.Outlined.SkipNext,
                label = "Next",
                active = false,
                enabled = hasNext,
                modifier = Modifier.weight(1f),
                onClick = onNext
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel(
            icon = Icons.Outlined.GraphicEq,
            title = "Playback speed",
            trailing = formatSpeed(speed)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SPEEDS.forEach { option ->
                SpeedChip(
                    label = formatSpeed(option),
                    selected = speed == option,
                    onClick = { onSpeed(option) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel(
            icon = Icons.Outlined.Subtitles,
            title = "Subtitles",
            trailing = subtitleName?.let { "External" } ?: "Auto"
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SpeedChip(
                label = "Off",
                selected = selectedTrack == null && subtitleName == null,
                onClick = onSubtitlesOff
            )
            subtitleTracks.forEach { track ->
                SpeedChip(
                    label = track.label,
                    selected = selectedTrack == track.label,
                    onClick = { onSelectTrack(track) }
                )
            }
            SpeedChip(
                label = if (subtitleName == null) "Load file" else "Replace file",
                selected = false,
                onClick = onLoadSubtitle
            )
        }

        if (subtitleName != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Loaded: $subtitleName",
                fontSize = 10.sp,
                color = PlayerGreen.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else if (subtitleTracks.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "No subtitle track found. Load an .srt, .vtt or .ass file.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.40f),
                lineHeight = 14.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Tip: double-tap the left or right side to skip 10 seconds. Swipe up or down for brightness and volume.",
            fontSize = 9.5.sp,
            color = Color.White.copy(alpha = 0.30f),
            lineHeight = 13.sp
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ToolTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accent = if (active) PlayerGreen else Color.White
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (active) PlayerGreen.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (active) PlayerGreen.copy(alpha = 0.42f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(15.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.copy(alpha = if (enabled) 0.92f else 0.30f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.copy(alpha = if (enabled) 0.92f else 0.30f),
                    maxLines = 2,
                    lineHeight = 13.sp
                )
                if (active) {
                    Text(
                        text = "On",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailing: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = trailing,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.40f)
        )
    }
}

@Composable
private fun SpeedChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.40f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.62f),
            maxLines = 1
        )
    }
}

@Composable
private fun PlayerActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.08f)
                    primary -> Color.White
                    else -> Color.White.copy(alpha = 0.10f)
                }
            )
            .border(
                1.dp,
                if (primary) Color.Transparent else Color.White.copy(alpha = 0.20f),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.30f)
                    primary -> Color.Black
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
                    primary -> Color.Black
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

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun formatSpeed(value: Float): String = when {
    value == value.toInt().toFloat() -> "${value.toInt()}x"
    else -> "${value}x"
}

private fun subtitleMime(name: String): String = when {
    name.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
    name.endsWith(".ass", ignoreCase = true) ||
        name.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
    else -> MimeTypes.APPLICATION_SUBRIP
}

private fun displayNameOf(context: Context, uri: Uri): String {
    val fromProvider = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
    return fromProvider?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "Subtitle"
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
