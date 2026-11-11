package com.vidvibe.app.floating

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.formatFileSize
import com.vidvibe.app.ui.components.CustomBottomNav
import com.vidvibe.app.ui.components.VidVibeToastHost
import com.vidvibe.app.ui.screens.DownloaderScreen
import com.vidvibe.app.ui.screens.LocalScreen
import com.vidvibe.app.ui.screens.MeScreen
import com.vidvibe.app.ui.screens.TrashScreen
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.LocalMenuPalette
import com.vidvibe.app.ui.viewmodel.MainViewModel

private val WindowShape = RoundedCornerShape(32.dp)

/**
 * The whole app, rendered inside a movable, resizable glass card. This is the
 * content of the overlay window; the service owns the window itself.
 */
@Composable
fun FloatingAppShell(
    viewModel: MainViewModel,
    settings: AppSettings,
    compact: Boolean,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onResize: (Float, Float) -> Unit,
    onResizeEnd: () -> Unit,
    onOpenInApp: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val trash by viewModel.trash.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val pasteToken by viewModel.pasteToken.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectionActive by remember { mutableStateOf(false) }
    var downloaderFullScreen by remember { mutableStateOf(false) }
    var profileSubPage by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val navHidden = selectionActive || downloaderFullScreen || profileSubPage
    val navHideFraction = if (selectionActive || profileSubPage) 2f else 1.5f

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val appBackground = LocalAppBackground.current

    if (compact) {
        CompactBubble(
            onExpand = onCollapse,
            onClose = onClose,
            onDrag = onDrag,
            onDragEnd = onDragEnd
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 26.dp, shape = WindowShape, clip = false)
            .clip(WindowShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xF0141418),
                        Color(0xF208080B)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = WindowShape
            )
    ) {
        WindowHeader(
            selectedTab = selectedTab,
            onBack = { dispatcher?.onBackPressed() },
            onCollapse = onCollapse,
            onClose = onClose,
            onDrag = onDrag,
            onDragEnd = onDragEnd
        )

        Box(modifier = Modifier.weight(1f)) {
            // The screens paint LocalAppBackground themselves; keep it
            // translucent so the glass card still reads as glass.
            CompositionLocalProvider(
                LocalAppBackground provides appBackground.copy(alpha = 0.90f)
            ) {
                Box(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> LocalScreen(
                            videos = videos,
                            isLoading = isLoading,
                            playlists = playlists,
                            onOpenVideo = { _, _ -> onOpenInApp() },
                            onRefresh = viewModel::refresh,
                            onSelectionModeChanged = { selectionActive = it },
                            onMoveToTrash = { items -> viewModel.moveToTrash(items) },
                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                            onAddVideosToPlaylist = { playlistId, ids ->
                                viewModel.addVideosToPlaylist(playlistId, ids)
                            },
                            onDeletePlaylist = { playlistId ->
                                viewModel.deletePlaylist(playlistId)
                            },
                            showToast = { toast = it }
                        )

                        1 -> DownloaderScreen(
                            downloadState = downloadState,
                            settings = settings,
                            onStartDownload = viewModel::startDownload,
                            onResetDownload = viewModel::resetDownload,
                            onWifiOnlyChange = viewModel::setWifiOnly,
                            onFullScreenChanged = { downloaderFullScreen = it },
                            pasteToken = pasteToken,
                            onPasteConsumed = viewModel::consumePaste,
                            showToast = { toast = it }
                        )

                        2 -> TrashScreen(
                            items = trash,
                            onBack = { selectedTab = 0 },
                            onSelectionModeChanged = { selectionActive = it },
                            onRestore = { ids -> viewModel.restoreFromTrash(ids) },
                            onDeletePermanently = { ids -> viewModel.deletePermanently(ids) },
                            onLaunchDeleteConfirmation = { sender ->
                                val sent = runCatching {
                                    sender.sendIntent(context, 0, null, null, null)
                                }.isSuccess
                                if (sent) {
                                    toast = "Confirm the deletion in the system dialog."
                                } else {
                                    toast = "Open VidVibe to delete this video permanently."
                                }
                            },
                            showToast = { toast = it }
                        )

                        else -> MeScreen(
                            settings = settings,
                            librarySizeLabel = formatFileSize(videos.sumOf { it.sizeBytes }),
                            floatingWindowEnabled = settings.floatingWindowEnabled,
                            overlayPermissionGranted = true,
                            onFloatingWindowEnabledChange = { enabled ->
                                viewModel.setFloatingWindowEnabled(enabled)
                                if (!enabled) onClose()
                            },
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onApplyCustomization = { selection ->
                                viewModel.setCustomization(
                                    animationStyle = selection.animationStyle.key,
                                    appFont = selection.appFont.key,
                                    backgroundLook = selection.backgroundLook.key,
                                    textColor = selection.textColor.key,
                                    glassColor = selection.glassColor.key,
                                    appTheme = selection.appTheme.key
                                )
                            },
                            onSubPageChanged = { profileSubPage = it },
                            showToast = { toast = it }
                        )
                    }

                    CustomBottomNav(
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        liquidGlass = settings.liquidGlass,
                        visible = !navHidden,
                        hideFraction = navHideFraction,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            ResizeGrip(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                onResize = onResize,
                onResizeEnd = onResizeEnd
            )

            VidVibeToastHost(
                message = toast,
                onDismiss = { toast = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (navHidden) 18.dp else 92.dp)
            )
        }
    }

    // Keep the window's own back stack predictable when the app is opened.
    LaunchedEffect(selectedTab) {
        if (selectedTab != 0) selectionActive = false
    }
}

@Composable
private fun WindowHeader(
    selectedTab: Int,
    onBack: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    val palette = LocalMenuPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount.x, amount.y)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedTab != 0) {
            GlassIconButton(
                icon = Icons.Outlined.KeyboardArrowLeft,
                description = "Back",
                onClick = onBack
            )
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF30D158), Color(0xFF1FA84A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF04140A),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "VidVibe",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        // iOS-style grabber: the visual affordance that the card can be moved.
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.highlight.copy(alpha = 0.55f))
            )
        }

        GlassIconButton(
            icon = Icons.Outlined.KeyboardArrowDown,
            description = "Minimize",
            onClick = onCollapse
        )
        Spacer(Modifier.width(6.dp))
        GlassIconButton(
            icon = Icons.Outlined.Close,
            description = "Close floating window",
            onClick = onClose
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 2400f),
        label = "window-chrome-press"
    )

    Box(
        modifier = Modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = textColor.copy(alpha = 0.80f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun ResizeGrip(
    modifier: Modifier = Modifier,
    onResize: (Float, Float) -> Unit,
    onResizeEnd: () -> Unit
) {
    val palette = LocalMenuPalette.current
    Box(
        modifier = modifier
            .size(26.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, amount ->
                        change.consume()
                        onResize(amount.x, amount.y)
                    },
                    onDragEnd = onResizeEnd,
                    onDragCancel = onResizeEnd
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(11.dp)) {
            val stroke = 1.6.dp.toPx()
            val color = palette.highlight.copy(alpha = 0.55f)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.15f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.55f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.55f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CompactBubble(
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f),
        label = "bubble-press"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(elevation = 18.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xF0161619),
                            Color(0xF0060608)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressed = false
                            }
                        },
                        onTap = { onExpand() }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.x, amount.y)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Open VidVibe window",
                tint = Color(0xFF30D158),
                modifier = Modifier.size(28.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xF01A1A1E))
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close floating window",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
