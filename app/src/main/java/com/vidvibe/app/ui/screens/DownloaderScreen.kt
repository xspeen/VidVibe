package com.vidvibe.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.ui.components.FloatingMenuColor
import com.vidvibe.app.ui.components.FloatingMenuState
import com.vidvibe.app.ui.components.FloatingPanel
import com.vidvibe.app.ui.components.VidVibeFloatingMenu
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.viewmodel.DownloadState
import kotlinx.coroutines.delay

@Composable
fun DownloaderScreen(
    downloadState: DownloadState,
    settings: AppSettings,
    onStartDownload: (String, Int) -> Unit,
    onResetDownload: () -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onFullScreenChanged: (Boolean) -> Unit = {},
    pasteToken: Int = 0,
    onPasteConsumed: () -> Unit = {},
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val menu = remember { FloatingMenuState() }

    var link by rememberSaveable { mutableStateOf("") }
    var selectedQuality by rememberSaveable { mutableIntStateOf(720) }

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

    fun openQuality() {
        keyboard?.hide()
        menu.openQuality()
    }

    fun beginDownload() {
        if (link.isBlank()) {
            val pasted = clipboardText()
            if (pasted.isNotBlank()) link = pasted
        }
        if (link.isBlank()) {
            showToast("Paste a video link first")
            return
        }
        openQuality()
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
                openQuality()
            } else {
                showToast("Clipboard is empty. Copy a video link first.")
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
                    text = "Media URL",
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
                    onFetch = { beginDownload() },
                    onGo = { beginDownload() }
                )
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
                    showToast("Paste a video link first")
                } else {
                    menu.beginProgress(auto = false, progress = 0f)
                    onStartDownload(link, selectedQuality)
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
