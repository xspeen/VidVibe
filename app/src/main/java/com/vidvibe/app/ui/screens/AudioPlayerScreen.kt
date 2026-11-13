package com.vidvibe.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.download.AudioEngine
import com.vidvibe.app.download.AudioPlaybackState
import com.vidvibe.app.ui.components.LocalMeshEnabled
import com.vidvibe.app.ui.theme.LocalGlassStyle
import com.vidvibe.app.data.formatDuration

private val ClayTop = Color(0xFF2C2924)
private val ClayBottom = Color(0xFF181613)
private val ClayGreen = Color(0xFF30D158)
private val ClayGreenDeep = Color(0xFF1C9E49)

/**
 * Full-screen audio player. Everything is driven by [AudioEngine] so the same
 * state feeds the lock-screen controls and the floating mini player.
 */
@Composable
fun AudioPlayerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by AudioEngine.state.collectAsState()
    val track = state.current

    BackHandler { onBack() }

    // If the queue is torn down under us (service stopped, last track
    // removed), don't leave the user on a dead "Nothing playing" screen.
    LaunchedEffect(track == null) {
        if (track == null) {
            delay(900)
            onBack()
        }
    }

    if (track == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0E)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nothing playing",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
        return
    }

    val spin = rememberInfiniteTransition(label = "album-spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing)
        ),
        label = "album-angle"
    )

    // Flat clay surface normally; with the mesh on the colour-sampled album
    // cover already sits behind this screen, so only frosted panels are drawn.
    val meshOn = LocalMeshEnabled.current
    val glass = LocalGlassStyle.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (meshOn) {
                    Modifier
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF141311), Color(0xFF070708))
                        )
                    )
                }
            )
    ) {
        // Soft clay glow behind the artwork.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(ClayGreen.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(150f, 150f),
                        radius = 320f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(glass.bodyTop)
                        .border(1.dp, glass.stroke, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = Color.White.copy(alpha = 0.42f)
                    )
                    Text(
                        text = "VidVibe Music",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ClayGreen.copy(alpha = 0.14f))
                        .border(1.dp, ClayGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${state.index + 1}/${state.queue.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF7CFFCB)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            AlbumArt(
                track = track,
                angle = if (state.isPlaying) angle else 0f,
                playing = state.isPlaying
            )

            Spacer(Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.displayName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = track.artist.ifBlank { "Unknown artist" },
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(22.dp))

            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = { AudioEngine.seekTo(context, it) }
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportButton(
                    icon = Icons.Outlined.SkipPrevious,
                    description = "Previous track",
                    size = 54.dp,
                    iconSize = 26.dp,
                    enabled = state.hasPrevious || state.positionMs > 4000L,
                    onClick = { AudioEngine.previous(context) }
                )
                PlayPauseClay(
                    playing = state.isPlaying,
                    onClick = { AudioEngine.toggle(context) }
                )
                TransportButton(
                    icon = Icons.Outlined.SkipNext,
                    description = "Next track",
                    size = 54.dp,
                    iconSize = 26.dp,
                    enabled = state.hasNext,
                    onClick = { AudioEngine.next(context) }
                )
            }

            Spacer(Modifier.height(24.dp))

            QueueList(
                state = state,
                onPick = { AudioEngine.playIndex(context, it) }
            )
        }
    }
}

@Composable
private fun AlbumArt(track: VideoItem, angle: Float, playing: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .shadow(
                    elevation = 34.dp,
                    shape = RoundedCornerShape(38.dp),
                    ambientColor = ClayGreen,
                    spotColor = ClayGreen.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(38.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(ClayTop.copy(alpha = 0.80f), ClayBottom.copy(alpha = 0.66f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(38.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF0D0D10)),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = angle * 0.02f }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.28f),
                        modifier = Modifier.size(74.dp)
                    )
                }
                if (!playing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.34f))
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    val density = LocalDensity.current
    val total = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / total).coerceIn(0f, 1f)
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(fraction) }
    val shown = if (dragging) dragFraction else fraction

    Column(modifier = Modifier.padding(horizontal = 28.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(total) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragging = false
                            onSeek((dragFraction * total).toLong())
                        },
                        onDragCancel = { dragging = false }
                    ) { change, amount ->
                        change.consume()
                        dragFraction = (dragFraction + amount / size.width.toFloat())
                            .coerceIn(0f, 1f)
                    }
                }
                .pointerInput(total) {
                    detectTapGestures(
                        onTap = { offset ->
                            val target = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = target
                            onSeek((target * total).toLong())
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(shown)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(listOf(ClayGreenDeep, ClayGreen))
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(shown)
                    .height(4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(if (dragging) 15.dp else 11.dp)
                        .shadow(6.dp, CircleShape, ambientColor = ClayGreen, spotColor = ClayGreen)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration(if (dragging) (dragFraction * total).toLong() else positionMs),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.55f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatDuration(total),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun PlayPauseClay(playing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                ambientColor = ClayGreen,
                spotColor = ClayGreen.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF43E884), ClayGreenDeep)))
            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3A352D).copy(alpha = 0.62f),
                        Color(0xFF1E1B17).copy(alpha = 0.52f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) Color.White.copy(alpha = 0.86f)
            else Color.White.copy(alpha = 0.24f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun QueueList(
    state: AudioPlaybackState,
    onPick: (Int) -> Unit
) {
    val glass = LocalGlassStyle.current
    val panel = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(panel)
            .background(
                Brush.verticalGradient(listOf(glass.bodyTop, glass.bodyBottom))
            )
            .border(1.dp, glass.stroke, panel)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UP NEXT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
                color = Color.White.copy(alpha = 0.38f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${state.queue.size} tracks",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.38f)
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.queue, key = { it.id }) { track ->
                val index = state.queue.indexOf(track)
                val active = index == state.index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) Color.White.copy(alpha = 0.07f)
                            else Color.Transparent
                        )
                        .clickable { onPick(index) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.albumArtUri != null) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (active) ClayGreen else Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist.ifBlank { "Unknown artist" },
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.40f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = track.durationLabel,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.40f)
                    )
                }
            }
        }
    }
}

/**
 * Floating mini player shown above the bottom navigation while audio is
 * playing. Tapping it reopens the full player.
 */
@Composable
fun AudioMiniPlayer(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by AudioEngine.state.collectAsState()
    val track = state.current ?: return
    val shape = RoundedCornerShape(20.dp)
    val fraction = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val shown by animateFloatAsState(targetValue = fraction, animationSpec = tween(320), label = "mini-progress")

    Box(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = ClayGreen.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xB32E2A24), Color(0xB3171512))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(onClick = onOpen)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.albumArtUri != null) {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist.ifBlank { "VidVibe Music" },
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.46f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { AudioEngine.toggle(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Outlined.Pause
                        else Icons.Outlined.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable(enabled = state.hasNext) { AudioEngine.next(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = "Next track",
                        tint = if (state.hasNext) Color.White.copy(alpha = 0.86f)
                        else Color.White.copy(alpha = 0.24f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(ClayGreenDeep, ClayGreen)))
                )
            }
        }
    }
}
