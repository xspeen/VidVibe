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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun pasteFromClipboard() {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val pasted = manager
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (pasted.isBlank()) {
            showToast("Clipboard is empty")
        } else {
            link = pasted
        }
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
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Downloader",
                    style = MaterialTheme.typography.titleLarge,
                    color = VidVibeText,
                    modifier = Modifier.weight(1f)
                )
                RoundIconButton(
                    onClick = { showQuality = link.isNotBlank(); if (link.isBlank()) showToast("Paste a video link first") },
                    icon = Icons.Outlined.Info,
                    contentDescription = "Download options"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Paste a link from a social platform. VidVibe downloads the " +
                        "video and saves it to your gallery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VidVibeTextSecondary,
                    modifier = Modifier.widthIn(max = 560.dp)
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = VidVibeTextTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "VIDEO LINK",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = VidVibeTextTertiary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { pasteFromClipboard() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    tint = VidVibeAccentRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Paste",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VidVibeAccentRed
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF13141A), RoundedCornerShape(12.dp))
                                .border(1.dp, VidVibeBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = link,
                                onValueChange = { link = it.trimStart() },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    color = VidVibeText,
                                    fontSize = 14.sp,
                                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily
                                ),
                                cursorBrush = SolidColor(VidVibeAccentRed),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(onGo = { beginDownload() }),
                                decorationBox = { inner ->
                                    Box {
                                        if (link.isEmpty()) {
                                            Text(
                                                text = "https://...",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                                color = VidVibeTextTertiary
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            if (link.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear link",
                                    tint = VidVibeTextTertiary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            link = ""
                                            if (downloadState !is DownloadState.Running) {
                                                onResetDownload()
                                            }
                                        }
                                )
                            }
                        }

                        if (host.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Source: $host",
                                style = MaterialTheme.typography.labelSmall,
                                color = VidVibeTextTertiary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWifiOnlyChange(!settings.wifiOnly) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = VidVibeTextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Wi-Fi only",
                                style = MaterialTheme.typography.bodyLarge,
                                color = VidVibeText,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Block downloads on mobile data",
                                style = MaterialTheme.typography.bodySmall,
                                color = VidVibeTextTertiary
                            )
                        }
                        Switch(
                            checked = settings.wifiOnly,
                            onCheckedChange = { onWifiOnlyChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VidVibeRed,
                                uncheckedThumbColor = VidVibeTextSecondary,
                                uncheckedTrackColor = VidVibeSurface3,
                                uncheckedBorderColor = VidVibeBorder
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(VidVibeSurface3, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                tint = VidVibeTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Saved to Movies/VidVibe",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VidVibeText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Downloads use progressive MP4 streams, so no " +
                                    "extra converter is needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VidVibeTextTertiary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        AnimatedVisibility(
            visible = link.isNotBlank() &&
                !showQuality &&
                downloadState is DownloadState.Idle,
            enter = scaleIn(
                initialScale = 0.4f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f)
            ) + fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 22.dp)
        ) {
            BouncingDownloadFab(onClick = { beginDownload() })
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
private fun BouncingDownloadFab(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "download-fab")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "download-fab-lift"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { translationY = lift.dp.toPx() }
            .size(64.dp)
            .shadow(18.dp, CircleShape)
            .background(
                Brush.linearGradient(listOf(VidVibeAccentRed, Color(0xFFB71C1C))),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = "Download video",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
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
                    "VidVibe picks a progressive stream so video and audio stay in sync.",
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
                                text = "${running.progress}",
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
                    Text(
                        text = running.status,
                        style = MaterialTheme.typography.titleMedium,
                        color = VidVibeText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${host.ifBlank { "Video" }} · ${quality}p",
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "Keep VidVibe open while the video downloads.",
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
