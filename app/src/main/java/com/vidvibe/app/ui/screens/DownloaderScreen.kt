package com.vidvibe.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.Platform
import com.vidvibe.app.download.DownloadTask
import com.vidvibe.app.download.QueueStatus
import com.vidvibe.app.ui.components.FloatingMenuColor
import com.vidvibe.app.ui.components.FloatingMenuState
import com.vidvibe.app.ui.components.FloatingPanel
import com.vidvibe.app.ui.components.PlatformLogo
import com.vidvibe.app.ui.components.VidVibeFloatingMenu
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.LocalGlassStyle
import com.vidvibe.app.ui.viewmodel.DownloadState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DownloaderScreen(
    downloadState: DownloadState,
    settings: AppSettings,
    onStartDownload: (String, Int, Boolean) -> Unit,
    onResetDownload: () -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onFullScreenChanged: (Boolean) -> Unit = {},
    pasteToken: Int = 0,
    onPasteConsumed: () -> Unit = {},
    queue: List<DownloadTask> = emptyList(),
    onCancelTask: (Long) -> Unit = {},
    onRetryTask: (Long) -> Unit = {},
    onRemoveTask: (Long) -> Unit = {},
    onClearFinished: () -> Unit = {},
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val menu = remember { FloatingMenuState() }

    var link by rememberSaveable { mutableStateOf("") }
    var selectedQuality by rememberSaveable { mutableIntStateOf(720) }
    var audioOnly by rememberSaveable { mutableStateOf(false) }
    val platform = remember(link) {
        if (link.isBlank()) Platform.Other else Platform.fromUrl(link)
    }

    LaunchedEffect(platform) {
        if (platform.audio && link.isNotBlank()) audioOnly = true
    }

    val overlayVisible = menu.success ||
        menu.panel == FloatingPanel.Quality ||
        menu.panel == FloatingPanel.Slide ||
        menu.panel == FloatingPanel.Progress

    LaunchedEffect(overlayVisible) {
        onFullScreenChanged(overlayVisible)
    }

    DisposableEffect(Unit) {
        onDispose {
            onFullScreenChanged(false)
            menu.hide()
        }
    }

    fun clipboardText(): String {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return manager
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()
    }

    fun openDownloadSheet() {
        keyboard?.hide()
        if (audioOnly) {
            menu.openSlide(
                color = FloatingMenuColor.Download,
                label = "Swipe to Download Audio",
                autoProgress = false
            )
        } else {
            menu.openQuality()
        }
    }

    fun beginDownload() {
        if (link.isBlank()) {
            val pasted = clipboardText()
            if (pasted.isNotBlank()) {
                link = pasted
                audioOnly = Platform.fromUrl(pasted).audio
            }
        }
        if (link.isBlank()) {
            showToast("Paste a link first")
            return
        }
        openDownloadSheet()
    }

    LaunchedEffect(pasteToken) {
        if (pasteToken != 0) {
            // The overlay brings the activity forward; give the window time to gain
            // focus before reading the clipboard (Android 10+ blocks background reads).
            var pasted = clipboardText()
            var attempts = 0
            while (pasted.isBlank() && attempts < 6) {
                delay(150)
                pasted = clipboardText()
                attempts += 1
            }
            if (pasted.isNotBlank()) {
                link = pasted
                audioOnly = Platform.fromUrl(pasted).audio
                openDownloadSheet()
            } else {
                showToast("Clipboard is empty. Copy a link first.")
            }
            onPasteConsumed()
        }
    }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Running -> {
                if (menu.panel == FloatingPanel.Progress) {
                    menu.updateProgress(state.progress / 100f)
                } else {
                    menu.beginProgress(auto = false, progress = state.progress / 100f)
                }
            }

            is DownloadState.Complete -> {
                menu.updateProgress(1f)
                delay(450)
                menu.showSuccess()
            }

            is DownloadState.Error -> {
                menu.hide()
                showToast(state.message)
                onResetDownload()
            }

            DownloadState.Idle -> Unit
        }
    }

    BackHandler(enabled = menu.panel != FloatingPanel.Hidden || menu.success) {
        menu.hide()
        if (downloadState !is DownloadState.Running) onResetDownload()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            DownloadHero()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "MEDIA URL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA0A0A8),
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                DownloadLinkInput(
                    link = link,
                    onLinkChange = {
                        link = it.trimStart()
                        if (it.isBlank()) audioOnly = false
                    },
                    onFetch = { beginDownload() },
                    onGo = { beginDownload() }
                )

                Spacer(Modifier.height(14.dp))
                PlatformRow(
                    platform = platform,
                    hasLink = link.isNotBlank(),
                    audioOnly = audioOnly,
                    onAudioToggle = {
                        audioOnly = !audioOnly
                        if (audioOnly) {
                            menu.openSlide(
                                color = FloatingMenuColor.Download,
                                label = "Swipe to Download Audio",
                                autoProgress = false
                            )
                        }
                    }
                )

                if (queue.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    DownloadQueuePanel(
                        queue = queue,
                        onCancel = onCancelTask,
                        onRetry = onRetryTask,
                        onRemove = onRemoveTask,
                        onClearFinished = onClearFinished
                    )
                }
                Spacer(Modifier.height(120.dp))
            }
        }

        VidVibeFloatingMenu(
            state = menu,
            onShare = {},
            onAddTo = {},
            onDelete = {},
            onRestore = {},
            onDeletePermanently = {},
            onCancel = { menu.hide() },
            onQualityPicked = { height ->
                selectedQuality = height
                menu.openSlide(
                    color = FloatingMenuColor.Download,
                    label = "Swipe to Download ${height}p",
                    autoProgress = false
                )
            },
            onSlideConfirmed = {
                if (link.isBlank()) {
                    menu.hide()
                    showToast("Paste a link first")
                } else {
                    menu.beginProgress(auto = false, progress = 0f)
                    onStartDownload(link, selectedQuality, audioOnly)
                }
            },
            onAutoProgressFinished = {},
            onSuccessFinished = {
                onResetDownload()
                menu.hide()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PlatformRow(
    platform: Platform,
    hasLink: Boolean,
    audioOnly: Boolean,
    onAudioToggle: () -> Unit
) {
    val glass = LocalGlassStyle.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasLink && platform != Platform.Other) {
            PlatformLogo(platform = platform, size = 22.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                text = platform.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(Modifier.weight(1f))
        } else {
            Text(
                text = "Video and audio links are supported",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (audioOnly) Color(0xFF30D158).copy(alpha = 0.18f)
                    else Color.White.copy(alpha = 0.055f)
                )
                .border(
                    1.dp,
                    if (audioOnly) Color(0xFF30D158).copy(alpha = 0.5f) else glass.stroke,
                    RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onAudioToggle)
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (audioOnly) Icons.Outlined.GraphicEq else Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = if (audioOnly) Color(0xFF30D158) else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (audioOnly) "Audio" else "Audio off",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (audioOnly) Color(0xFF7CFFCB) else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DownloadHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(Color(0xFF0A0A0C))
        ) {
            AsyncImage(
                model = "https://iili.io/nteYKe1.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            1f to Color(0xD90A0A0C)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "VidVibe",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Text(
                text = "Paste a link to save",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun DownloadLinkInput(
    link: String,
    onLinkChange: (String) -> Unit,
    onFetch: () -> Unit,
    onGo: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val active = focused || link.isNotEmpty()

    val elevator = rememberInfiniteTransition(label = "elevator")
    val t by elevator.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0f at 0
                0.4f at 600
                0.41f at 610
                0.7f at 1050
                1f at 1500
            }
        ),
        label = "elevator-t"
    )
    val elevatorOffset = when {
        t < 0.4f -> -20f * (t / 0.4f)
        t < 0.41f -> 20f
        t < 0.7f -> 20f * (1f - (t - 0.41f) / 0.29f)
        else -> 0f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xCC14141A), Color(0xB30A0A0C))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (active) Color(0xFF4E9BFF)
                        else Color.White.copy(alpha = 0.18f),
                        if (active) Color(0x994E9BFF)
                        else Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        translationY = if (active) elevatorOffset.dp.toPx() else 0f
                    }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    tint = if (active) Color(0xFF4E9BFF) else Color(0xFF6B6B75),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = link,
            onValueChange = onLinkChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Color(0xFF4E9BFF)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            decorationBox = { inner ->
                Box {
                    if (link.isEmpty()) {
                        Text(
                            text = "Paste a video or music link...",
                            fontSize = 14.sp,
                            color = Color(0xFF6B6B75),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(onClick = onFetch)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Fetch",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun DownloadQueuePanel(
    queue: List<DownloadTask>,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClearFinished: () -> Unit
) {
    val glass = LocalGlassStyle.current
    val finished = queue.count { it.isFinished }
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF17171E), Color(0xFF0D0D12))
                )
            )
            .border(1.dp, glass.stroke, shape)
            .padding(horizontal = 12.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "QUEUE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.42f),
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${queue.size}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.66f)
                )
            }
            Spacer(Modifier.weight(1f))
            if (finished > 0) {
                Text(
                    text = "Clear finished",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7FB9FF),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(onClick = onClearFinished)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        queue.forEach { task ->
            QueueCard(
                task = task,
                onCancel = { onCancel(task.id) },
                onRetry = { onRetry(task.id) },
                onRemove = { onRemove(task.id) }
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(1.dp))
    }
}

@Composable
private fun QueueCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val accent = when (task.state) {
        QueueStatus.Running -> Color(0xFF5AA9FF)
        QueueStatus.Queued -> Color(0xFF9AA0AA)
        QueueStatus.Complete -> Color(0xFF30D158)
        QueueStatus.Failed -> Color(0xFFFF5A5F)
        QueueStatus.Cancelled -> Color(0xFF9AA0AA)
    }
    val shape = RoundedCornerShape(18.dp)
    val running = task.state == QueueStatus.Running || task.state == QueueStatus.Queued
    val retryable = task.state == QueueStatus.Failed || task.state == QueueStatus.Cancelled
    val complete = task.state == QueueStatus.Complete
    val primaryLabel = if (running) "Cancel" else "Remove"
    val primaryAction = if (running) onCancel else onRemove

    var dragging by remember(task.id) { mutableStateOf(false) }
    var dragX by remember(task.id) { mutableFloatStateOf(0f) }
    val maxSwipePx = with(density) { 104.dp.toPx() }
    val threshold = with(density) { 84.dp.toPx() }
    val settled by animateFloatAsState(
        targetValue = if (dragging) dragX else 0f,
        animationSpec = if (dragging) {
            androidx.compose.animation.core.snap()
        } else {
            tween(durationMillis = 220, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
        },
        label = "queue-swipe"
    )
    val progress by animateFloatAsState(
        targetValue = if (complete) 1f else task.progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(420),
        label = "queue-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (retryable) Color(0xFF0E3A24) else Color.Transparent,
                            Color.Transparent,
                            if (running) Color(0xFF3A1518) else Color(0xFF1B1B22)
                        )
                    )
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (retryable) {
                Text(
                    text = "Retry",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7CFFCB)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = primaryLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (running) Color(0xFFFF8A8E) else Color.White.copy(alpha = 0.62f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(settled.roundToInt(), 0) }
                .clip(shape)
                .background(Color(0xFF111116))
                .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
                .pointerInput(task.id, task.state) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            when {
                                dragX <= -threshold -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    primaryAction()
                                }
                                dragX >= threshold && retryable -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRetry()
                                }
                            }
                            dragX = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            dragX = 0f
                        }
                    ) { change, amount ->
                        change.consume()
                        dragX = (dragX + amount).coerceIn(-maxSwipePx, maxSwipePx)
                    }
                }
        ) {
            // Full-width design loader: the whole card fills up.
            Box(Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            when {
                                complete -> Brush.horizontalGradient(
                                    listOf(Color(0xFF1E7A46), Color(0xFF30D158))
                                )
                                retryable -> Brush.horizontalGradient(
                                    listOf(Color(0xFF143226), Color(0xFF1E4A36))
                                )
                                else -> Brush.horizontalGradient(
                                    listOf(Color(0xFF123A66), Color(0xFF1E5E9E))
                                )
                            }
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (complete) {
                        QueueSuccessMark()
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.24f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlatformLogo(platform = task.platform, size = 26.dp)
                        }
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = when (task.state) {
                            QueueStatus.Queued -> "Waiting in queue"
                            QueueStatus.Complete -> "Saved · ${task.platform.label}"
                            QueueStatus.Failed -> task.error ?: "Download failed"
                            QueueStatus.Cancelled -> "Cancelled"
                            QueueStatus.Running -> task.status
                        },
                        fontSize = 10.sp,
                        color = if (task.state == QueueStatus.Failed) {
                            Color(0xFFFFB3B6)
                        } else {
                            Color.White.copy(alpha = 0.68f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                when {
                    complete -> {
                        Text(
                            text = "100%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    running -> {
                        Text(
                            text = "${task.progress.coerceIn(0, 100)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    if (task.state == QueueStatus.Complete) onRemove()
                                    else onRetry()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (task.state == QueueStatus.Complete) {
                                    Icons.Outlined.Close
                                } else {
                                    Icons.Outlined.Refresh
                                },
                                contentDescription = if (task.state == QueueStatus.Complete) {
                                    "Remove from list"
                                } else {
                                    "Retry download"
                                },
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slow-motion green tick with a soft glow and sparkles, drawn inside the
 * finished queue card exactly like the share drawer's success state.
 */
@Composable
private fun QueueSuccessMark() {
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            glow.animateTo(1f, tween(420))
        }
        launch {
            check.animateTo(
                1f,
                tween(durationMillis = 900, easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f))
            )
        }
        launch {
            delay(160)
            burst.animateTo(
                1f,
                tween(durationMillis = 1200, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
            )
        }
    }

    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        repeat(8) { index ->
            val angle = (index / 8f) * (Math.PI * 2f).toFloat()
            val distance = 22f + (index % 3) * 6f
            val dx = cos(angle) * distance * burst.value
            val dy = sin(angle) * distance * burst.value
            val remaining = 1f - burst.value
            Box(
                modifier = Modifier
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(4.dp)
                    .graphicsLayer {
                        scaleX = remaining
                        scaleY = remaining
                        alpha = remaining
                        rotationZ = 360f * burst.value
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (index % 2 == 0) Color(0xFFD6FFE6) else Color(0xFFFFE58A)
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = 0.75f + glow.value * 0.25f
                    scaleY = 0.75f + glow.value * 0.25f
                }
                .clip(CircleShape)
                .background(Color(0xFF0E3A24))
                .border(1.dp, Color(0xFF30D158).copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(24.dp)) {
                val s = size.width / 26f
                val path = Path().apply {
                    moveTo(6f * s, 13.5f * s)
                    lineTo(10.5f * s, 18f * s)
                    lineTo(20f * s, 8f * s)
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
                    color = Color(0xFF30D158),
                    style = Stroke(
                        width = 2.6f * s,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
