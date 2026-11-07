package com.vidvibe.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.ui.components.RoundIconButton
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.VidVibeAccentRed
import com.vidvibe.app.ui.theme.VidVibeBlue
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeGreen
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import com.vidvibe.app.ui.viewmodel.DownloadState
import kotlin.math.cos
import kotlin.math.sin

private data class QualityOption(
    val height: Int,
    val title: String,
    val subtitle: String
)

private val qualityOptions = listOf(
    QualityOption(2160, "4K", "Highest detail when the source allows it"),
    QualityOption(1440, "2K", "Sharp quality for larger screens"),
    QualityOption(1080, "Full HD", "1080p when the source allows it"),
    QualityOption(720, "HD", "Balanced quality and file size"),
    QualityOption(480, "SD", "Smaller file, faster download"),
    QualityOption(360, "Low", "Saves data on slow connections")
)

@Composable
fun DownloaderScreen(
    downloadState: DownloadState,
    settings: AppSettings,
    onStartDownload: (String, Int) -> Unit,
    onResetDownload: () -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onFullScreenChanged: (Boolean) -> Unit = {},
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    var link by rememberSaveable { mutableStateOf("") }
    var showQuality by remember { mutableStateOf(false) }
    var selectedQuality by rememberSaveable { mutableIntStateOf(720) }

    val showProgress = downloadState !is DownloadState.Idle && !showQuality
    val host = remember(link) {
        runCatching { android.net.Uri.parse(link.trim()).host }
            .getOrNull()
            ?.removePrefix("www.")
            .orEmpty()
    }

    LaunchedEffect(showQuality, showProgress) {
        onFullScreenChanged(showQuality || showProgress)
    }

    DisposableEffect(Unit) {
        onDispose { onFullScreenChanged(false) }
    }

    fun beginDownload() {
        keyboard?.hide()
        if (link.isBlank()) {
            showToast("Paste a video link first")
            return
        }
        showQuality = true
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

    BackHandler(enabled = showQuality) { showQuality = false }
    BackHandler(enabled = showProgress) {
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
                    text = "Paste Link",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA0A0A8),
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                DownloadLinkInput(
                    link = link,
                    onLinkChange = { link = it.trimStart() },
                    onPaste = {
                        val pasted = clipboardText()
                        when {
                            pasted.isNotBlank() -> {
                                link = pasted
                                beginDownload()
                            }
                            link.isNotBlank() -> beginDownload()
                            else -> showToast("Clipboard is empty")
                        }
                    },
                    onClear = {
                        link = ""
                        if (downloadState !is DownloadState.Running) {
                            onResetDownload()
                        }
                    },
                    onGo = { beginDownload() }
                )
                if (host.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Source: $host",
                        fontSize = 11.sp,
                        color = Color(0xFF6B6B75),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(Modifier.height(120.dp))
            }
        }

        AnimatedVisibility(
            visible = showQuality,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            )
        ) {
            QualityPage(
                link = link,
                host = host,
                selectedQuality = selectedQuality,
                onSelectQuality = { selectedQuality = it },
                onBack = { showQuality = false },
                onDownload = {
                    showQuality = false
                    onStartDownload(link, selectedQuality)
                }
            )
        }

        AnimatedVisibility(
            visible = showProgress,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            DownloadProgressPage(
                state = downloadState,
                link = link,
                quality = selectedQuality,
                onDone = {
                    onResetDownload()
                    showToast("Saved to Movies/VidVibe")
                },
                onClose = { onResetDownload() },
                onRetry = { onStartDownload(link, selectedQuality) }
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
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF3A2416), Color(0xFF0A0A0C))
                    )
                )
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
                            listOf(
                                Color(0x66000000),
                                Color.Transparent,
                                Color(0xE60A0A0C)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
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
                text = "Paste a link to download",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
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
    onPaste: () -> Unit,
    onClear: () -> Unit,
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
                    .graphicsLayer { translationY = elevatorOffset.dp.toPx() }
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
                            text = "https://www.instagram.com/reel/...",
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
        if (link.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear link",
                    tint = Color(0xFFA0A0A8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(onClick = onPaste)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Paste",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun QualityPage(
    link: String,
    host: String,
    selectedQuality: Int,
    onSelectQuality: (Int) -> Unit,
    onBack: () -> Unit,
    onDownload: () -> Unit
) {
    val background = LocalAppBackground.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = VidVibeText
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Choose quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = VidVibeText,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Best available quality up to your selection. " +
                    "Video and audio are combined on your device so the saved file plays smoothly.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeTextSecondary,
                modifier = Modifier.widthIn(max = 560.dp)
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                color = VidVibeSurface2,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = VidVibeTextTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (host.isBlank()) link else host,
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                qualityOptions.forEach { option ->
                    val selected = option.height == selectedQuality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) VidVibeSurface3 else VidVibeSurface2,
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) VidVibeAccentRed else VidVibeBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectQuality(option.height) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = VidVibeText,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.07f),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = "${option.height}p",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = VidVibeTextSecondary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = VidVibeTextTertiary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(if (selected) VidVibeAccentRed else Color.Transparent)
                                .border(
                                    2.dp,
                                    if (selected) VidVibeAccentRed else VidVibeTextTertiary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .widthIn(max = 640.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VidVibeRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Download video",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DownloadProgressPage(
    state: DownloadState,
    link: String,
    quality: Int,
    onDone: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit
) {
    val background = LocalAppBackground.current
    val host = remember(link) {
        runCatching { android.net.Uri.parse(link.trim()).host }
            .getOrNull()
            ?.removePrefix("www.")
            .orEmpty()
    }
    val running = state as? DownloadState.Running
    val progress = running?.progress ?: if (state is DownloadState.Complete) 100 else 0
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(480, easing = FastOutSlowInEasing),
        label = "download-progress"
    )
    val ambient = rememberInfiniteTransition(label = "download-ambient")
    val ambientShift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "download-ambient-shift"
    )
    val spin by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing)
        ),
        label = "download-spin"
    )
    val pulse by ambient.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "download-pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-90).dp)
                .size(320.dp)
                .graphicsLayer { alpha = 0.20f + ambientShift * 0.16f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            VidVibeBlue.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 90.dp)
                .size(300.dp)
                .graphicsLayer { alpha = 0.18f + (1f - ambientShift) * 0.14f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            VidVibeAccentRed.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            when (state) {
                is DownloadState.Running -> {
                    Box(
                        modifier = Modifier.size(216.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = spin }
                        ) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = (size.minDimension - 30.dp.toPx()) / 2f
                            for (index in 0 until 40) {
                                val angle = (index * 9f) * (Math.PI.toFloat() / 180f)
                                val strength = 0.06f + (index % 10) * 0.012f
                                drawCircle(
                                    color = Color.White.copy(alpha = strength),
                                    radius = if (index % 5 == 0) 2.1.dp.toPx() else 1.2.dp.toPx(),
                                    center = Offset(
                                        x = center.x + cos(angle) * radius,
                                        y = center.y + sin(angle) * radius
                                    )
                                )
                            }
                        }
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 11.dp.toPx()
                            val inset = stroke / 2f
                            drawCircle(
                                color = Color(0xFF21232D),
                                radius = (size.minDimension - stroke) / 2f,
                                style = Stroke(width = stroke)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(VidVibeBlue, VidVibeRed, VidVibeAccentRed)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - stroke,
                                    size.height - stroke
                                ),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.progress}",
                                style = MaterialTheme.typography.displayMedium,
                                color = VidVibeText,
                                fontWeight = FontWeight.Light
                            )
                            Text(
                                text = "PERCENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = VidVibeTextTertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .graphicsLayer { alpha = pulse }
                                .background(VidVibeGreen, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.status,
                            style = MaterialTheme.typography.titleMedium,
                            color = VidVibeText,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${host.ifBlank { "Video" }} · ${quality}p",
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "Video and audio are combined on your device. Keep VidVibe open while the download finishes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextTertiary,
                        textAlign = TextAlign.Center
                    )
                }

                is DownloadState.Complete -> {
                    SuccessTick()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Saved to gallery",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextTertiary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(26.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .widthIn(max = 420.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VidVibeGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Done", style = MaterialTheme.typography.labelLarge)
                    }
                }

                is DownloadState.Error -> {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(VidVibeRed.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, VidVibeRed.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = VidVibeRed,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Download failed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VidVibeTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 380.dp)
                    )
                    Spacer(Modifier.height(26.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(VidVibeSurface2, RoundedCornerShape(14.dp))
                        ) {
                            Text("Close", color = VidVibeTextSecondary)
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VidVibeRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Try again", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                else -> Unit
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SuccessTick() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "success-scale"
    )

    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val value = if (visible) 1f else 0f
            val center = Offset(size.width / 2f, size.height / 2f)
            for (index in 0 until 14) {
                val angle = (index * (360f / 14f)) * (Math.PI.toFloat() / 180f)
                val radius = 42.dp.toPx() + value * 20.dp.toPx()
                drawCircle(
                    color = VidVibeGreen.copy(alpha = 0.65f * value),
                    radius = if (index % 3 == 0) 2.6.dp.toPx() else 1.6.dp.toPx(),
                    center = Offset(
                        x = center.x + cos(angle) * radius,
                        y = center.y + sin(angle) * radius
                    )
                )
            }
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(VidVibeGreen.copy(alpha = 0.14f), CircleShape)
                .border(1.dp, VidVibeGreen.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = VidVibeGreen,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}
