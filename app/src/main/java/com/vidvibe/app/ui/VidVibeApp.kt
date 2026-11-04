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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.ui.components.AboutVidVibeDialog
import com.vidvibe.app.ui.components.AppDrawer
import com.vidvibe.app.ui.components.CustomBottomNav
import com.vidvibe.app.ui.components.VIDVIBE_GITHUB_URL
import com.vidvibe.app.ui.components.VidVibeToastHost
import com.vidvibe.app.ui.screens.DownloaderScreen
import com.vidvibe.app.ui.screens.LocalScreen
import com.vidvibe.app.ui.screens.MeScreen
import com.vidvibe.app.ui.screens.VideoPlayerScreen
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.viewmodel.MainViewModel

@Composable
fun VidVibeApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var drawerOpen by remember { mutableStateOf(false) }
    var playerVideo by remember { mutableStateOf<VideoItem?>(null) }
    var selectionActive by remember { mutableStateOf(false) }
    var downloaderFullScreen by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

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

    BackHandler(enabled = selectedTab != 0 && playerVideo == null && !drawerOpen) {
        selectedTab = 0
    }

    val appBackground = LocalAppBackground.current
    val showBottomNav = !selectionActive && playerVideo == null && !downloaderFullScreen

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> LocalScreen(
                        videos = videos,
                        isLoading = isLoading,
                        playlists = playlists,
                        onOpenVideo = { playerVideo = it },
                        onRefresh = viewModel::refresh,
                        onOpenDrawer = { drawerOpen = true },
                        onSelectionModeChanged = { selectionActive = it },
                        onDeleteVideos = { uris -> viewModel.deleteVideos(uris) },
                        onLaunchDeleteConfirmation = { intentSender ->
                            runCatching {
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }.onFailure {
                                showToast("Android could not open the deletion dialog.")
                            }
                        },
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
                    else -> MeScreen(
                        settings = settings,
                        videoCount = videos.size,
                        playlistCount = playlists.size,
                        onFontSizeChange = viewModel::setFontSize,
                        onLiquidGlassChange = viewModel::setLiquidGlass,
                        onBackgroundChange = viewModel::setBackgroundColor,
                        onWifiOnlyChange = viewModel::setWifiOnly,
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
            onDismiss = { drawerOpen = false },
            onSelectTab = { tab ->
                selectedTab = tab
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
                onBack = { playerVideo = null }
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
