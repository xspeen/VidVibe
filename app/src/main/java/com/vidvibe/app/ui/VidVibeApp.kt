package com.vidvibe.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.formatFileSize
import com.vidvibe.app.ui.components.AboutVidVibeDialog
import com.vidvibe.app.ui.components.AppDrawer
import com.vidvibe.app.ui.components.CustomBottomNav
import com.vidvibe.app.ui.components.VIDVIBE_GITHUB_URL
import com.vidvibe.app.ui.components.VidVibeToastHost
import com.vidvibe.app.ui.screens.DownloaderScreen
import com.vidvibe.app.ui.screens.LocalScreen
import com.vidvibe.app.ui.screens.MeScreen
import com.vidvibe.app.ui.screens.OnboardingScreen
import com.vidvibe.app.ui.screens.TrashScreen
import com.vidvibe.app.ui.screens.VideoPlayerScreen
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.viewmodel.MainViewModel

@Composable
fun VidVibeApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val trash by viewModel.trash.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var drawerOpen by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var playerQueue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var showTour by remember { mutableStateOf(false) }
    var selectionActive by remember { mutableStateOf(false) }
    var downloaderFullScreen by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val playerVideo = playerQueue.getOrNull(playerIndex)

    fun showToast(message: String) {
        toast = message
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteConfirmed()
            showToast("Videos deleted permanently")
        } else {
            viewModel.onSystemDeleteCancelled()
            showToast("Deletion cancelled")
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 0) selectionActive = false
    }

    DisposableEffect(selectedTab) {
        onDispose {
            if (selectedTab != 1) downloaderFullScreen = false
        }
    }

    BackHandler(enabled = selectedTab != 0 && playerVideo == null && !drawerOpen && !trashOpen && !showTour) {
        selectedTab = 0
    }

    val appBackground = LocalAppBackground.current
    val showBottomNav = !selectionActive &&
        playerVideo == null &&
        !downloaderFullScreen &&
        !trashOpen &&
        !showTour

    var rootWidth by remember { mutableStateOf(0) }
    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(300),
        label = "drawer-content-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { rootWidth = it.width }
                .graphicsLayer {
                    val shift = rootWidth * 0.78f * drawerProgress
                    translationX = -shift
                    val scale = 1f - 0.06f * drawerProgress
                    scaleX = scale
                    scaleY = scale
                }
                .clip(
                    RoundedCornerShape((22f * drawerProgress).dp)
                )
        ) {
            Box(Modifier.weight(1f)) {
                when {
                    trashOpen -> TrashScreen(
                        items = trash,
                        onBack = { trashOpen = false },
                        onRestore = { ids -> viewModel.restoreFromTrash(ids) },
                        onDeletePermanently = { ids -> viewModel.deletePermanently(ids) },
                        onLaunchDeleteConfirmation = { intentSender ->
                            runCatching {
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }.onFailure {
                                showToast("Android could not open the deletion dialog.")
                            }
                        },
                        showToast = { showToast(it) }
                    )
                    selectedTab == 0 -> LocalScreen(
                        videos = videos,
                        isLoading = isLoading,
                        playlists = playlists,
                        onOpenVideo = { queue, index ->
                            playerQueue = queue
                            playerIndex = index
                        },
                        onRefresh = viewModel::refresh,
                        onOpenDrawer = { drawerOpen = true },
                        onSelectionModeChanged = { selectionActive = it },
                        onMoveToTrash = { items -> viewModel.moveToTrash(items) },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onAddVideosToPlaylist = { playlistId, ids ->
                            viewModel.addVideosToPlaylist(playlistId, ids)
                        },
                        onDeletePlaylist = { playlistId ->
                            viewModel.deletePlaylist(playlistId)
                        },
                        showToast = { showToast(it) }
                    )
                    selectedTab == 1 -> DownloaderScreen(
                        downloadState = downloadState,
                        settings = settings,
                        onStartDownload = viewModel::startDownload,
                        onResetDownload = viewModel::resetDownload,
                        onWifiOnlyChange = viewModel::setWifiOnly,
                        onFullScreenChanged = { downloaderFullScreen = it },
                        showToast = { showToast(it) }
                    )
                    else -> MeScreen(
                        settings = settings,
                        videoCount = videos.size,
                        playlistCount = playlists.size,
                        librarySizeLabel = formatFileSize(videos.sumOf { it.sizeBytes }),
                        onFontSizeChange = viewModel::setFontSize,
                        onLiquidGlassChange = viewModel::setLiquidGlass,
                        onBackgroundChange = viewModel::setBackgroundColor,
                        onWifiOnlyChange = viewModel::setWifiOnly,
                        onAppLockChange = viewModel::setAppLock,
                        onReplayTour = { showTour = true },
                        showToast = { showToast(it) }
                    )
                }
            }

            if (showBottomNav) {
                CustomBottomNav(
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    liquidGlass = settings.liquidGlass
                )
            }
        }

        VidVibeToastHost(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showBottomNav) 76.dp else 26.dp)
        )

        AppDrawer(
            visible = drawerOpen,
            selectedTab = selectedTab,
            videoCount = videos.size,
            playlistCount = playlists.size,
            trashCount = trash.size,
            onDismiss = { drawerOpen = false },
            onSelectTab = { tab ->
                selectedTab = tab
                trashOpen = false
                drawerOpen = false
            },
            onOpenTrash = {
                trashOpen = true
                drawerOpen = false
            },
            onRefresh = {
                viewModel.refresh()
                showToast("Refreshing library")
            },
            onAbout = { showAbout = true },
            onOpenGitHub = {
                openUrl(context, VIDVIBE_GITHUB_URL) { showToast(it) }
            }
        )

        if (showAbout) {
            AboutVidVibeDialog(
                onDismiss = { showAbout = false },
                onOpenGitHub = {
                    showAbout = false
                    openUrl(context, VIDVIBE_GITHUB_URL) { showToast(it) }
                }
            )
        }

        playerVideo?.let { video ->
            VideoPlayerScreen(
                video = video,
                hasNext = playerIndex < playerQueue.lastIndex,
                hasPrevious = playerIndex > 0,
                onNext = {
                    if (playerIndex < playerQueue.lastIndex) playerIndex += 1
                },
                onPrevious = {
                    if (playerIndex > 0) playerIndex -= 1
                },
                onBack = {
                    playerQueue = emptyList()
                    playerIndex = -1
                }
            )
        }

        if (showTour) {
            OnboardingScreen(
                onFinish = { showTour = false }
            )
        }
    }
}

private fun openUrl(context: Context, url: String, onError: (String) -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        onError("No browser app is available.")
    }
}
