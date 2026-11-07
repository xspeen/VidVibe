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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.formatFileSize
import com.vidvibe.app.ui.components.AboutVidVibeDialog
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

    // 0 = Local, 1 = Downloader, 2 = Trash, 3 = Me
    var selectedTab by rememberSaveable { mutableStateOf(0) }
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

    BackHandler(enabled = selectedTab != 0 && playerVideo == null && !showTour) {
        selectedTab = 0
    }

    val appBackground = LocalAppBackground.current
    val showBottomNav = !selectionActive &&
        playerVideo == null &&
        !downloaderFullScreen &&
        !showTour

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        when (selectedTab) {
            0 -> LocalScreen(
                videos = videos,
                isLoading = isLoading,
                playlists = playlists,
                onOpenVideo = { queue, index ->
                    playerQueue = queue
                    playerIndex = index
                },
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
                showToast = { showToast(it) }
            )

            1 -> DownloaderScreen(
                downloadState = downloadState,
                settings = settings,
                onStartDownload = viewModel::startDownload,
                onResetDownload = viewModel::resetDownload,
                onWifiOnlyChange = viewModel::setWifiOnly,
                onFullScreenChanged = { downloaderFullScreen = it },
                showToast = { showToast(it) }
            )

            2 -> TrashScreen(
                items = trash,
                onBack = { selectedTab = 0 },
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

        if (showBottomNav) {
            CustomBottomNav(
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                liquidGlass = settings.liquidGlass,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        VidVibeToastHost(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showBottomNav) 124.dp else 26.dp)
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
