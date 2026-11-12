package com.vidvibe.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.formatFileSize
import com.vidvibe.app.download.AudioEngine
import com.vidvibe.app.download.DownloadTask
import com.vidvibe.app.download.QueueStatus
import com.vidvibe.app.ui.components.AboutVidVibeDialog
import com.vidvibe.app.ui.components.CustomBottomNav
import com.vidvibe.app.ui.components.DownloadSavedCard
import com.vidvibe.app.ui.components.VIDVIBE_GITHUB_URL
import com.vidvibe.app.ui.components.VidVibeToastHost
import com.vidvibe.app.ui.screens.AudioMiniPlayer
import com.vidvibe.app.ui.screens.AudioPlayerScreen
import com.vidvibe.app.ui.screens.DownloaderScreen
import com.vidvibe.app.ui.screens.LocalScreen
import com.vidvibe.app.ui.screens.MeScreen
import com.vidvibe.app.ui.screens.OnboardingScreen
import com.vidvibe.app.ui.screens.TrashScreen
import com.vidvibe.app.ui.screens.VideoPlayerScreen
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun VidVibeApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val audios by viewModel.audios.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val trash by viewModel.trash.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val pendingTab by viewModel.pendingTab.collectAsStateWithLifecycle()
    val pasteToken by viewModel.pasteToken.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    // 0 = Local, 1 = Downloader, 2 = Trash, 3 = Me
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var playerQueue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var playerIndex by remember { mutableIntStateOf(-1) }
    var showAudioPlayer by remember { mutableStateOf(false) }
    var showTour by remember { mutableStateOf(false) }
    var selectionActive by remember { mutableStateOf(false) }
    var downloaderFullScreen by remember { mutableStateOf(false) }
    var profileSubPage by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var celebration by remember { mutableStateOf<DownloadTask?>(null) }
    var celebratedId by remember { mutableStateOf(-1L) }
    val appStartedAt = remember { System.currentTimeMillis() }

    val playerVideo = playerQueue.getOrNull(playerIndex)

    fun showToast(message: String) {
        toast = message
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteConfirmed()
        } else {
            viewModel.onSystemDeleteCancelled()
        }
    }

    LaunchedEffect(pendingTab) {
        pendingTab?.let { tab ->
            selectedTab = tab
            viewModel.consumePendingTab()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 0) selectionActive = false
    }

    LaunchedEffect(queue) {
        val done = queue.lastOrNull {
            it.state == QueueStatus.Complete &&
                it.createdAt >= appStartedAt &&
                it.id != celebratedId
        }
        if (done != null) {
            celebratedId = done.id
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            celebration = done
        }
    }

    LaunchedEffect(celebration) {
        if (celebration != null) {
            delay(3400)
            celebration = null
        }
    }

    BackHandler(
        enabled = selectedTab != 0 && playerVideo == null && !showTour && !showAudioPlayer
    ) {
        selectedTab = 0
    }

    val appBackground = LocalAppBackground.current
    val navVisible = playerVideo == null && !showTour && !showAudioPlayer
    val navHidden = selectionActive || downloaderFullScreen || profileSubPage
    val navHideFraction = if (selectionActive || profileSubPage) 2f else 1.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        Box(Modifier.fillMaxSize()) {
            val tabContent: @Composable (Int) -> Unit = { tab ->
                when (tab) {
                    0 -> LocalScreen(
                        videos = videos,
                        audios = audios,
                        isLoading = isLoading,
                        playlists = playlists,
                        badges = badges,
                        onSectionSeen = viewModel::markSectionSeen,
                        onOpenAudio = { tracks, index ->
                            viewModel.playAudio(tracks, index)
                            showAudioPlayer = true
                        },
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
                        pasteToken = pasteToken,
                        onPasteConsumed = viewModel::consumePaste,
                        queue = queue,
                        onCancelTask = viewModel::cancelQueuedDownload,
                        onRetryTask = viewModel::retryQueuedDownload,
                        onRemoveTask = viewModel::removeQueuedDownload,
                        onClearFinished = viewModel::clearFinishedDownloads,
                        showToast = { showToast(it) }
                    )

                    2 -> TrashScreen(
                        items = trash,
                        onBack = { selectedTab = 0 },
                        onSelectionModeChanged = { selectionActive = it },
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
                        librarySizeLabel = formatFileSize(videos.sumOf { it.sizeBytes }),
                        onApplyCustomization = { selection ->
                            viewModel.setCustomization(
                                animationStyle = selection.animationStyle.key,
                                appFont = selection.appFont.key,
                                backgroundLook = selection.backgroundLook.key,
                                textColor = selection.textColor.key,
                                glassColor = selection.glassColor.key,
                                iconPack = selection.iconPack.key,
                                appTheme = selection.appTheme.key
                            )
                        },
                        onSubPageChanged = { profileSubPage = it },
                        showToast = { showToast(it) }
                    )
                }

            }
            val content: @Composable () -> Unit = {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val direction = if (forward) 1 else -1
                        (
                            fadeIn(
                                tween(
                                    durationMillis = 260,
                                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                                )
                            ) +
                                slideInHorizontally(
                                    animationSpec = tween(
                                        durationMillis = 360,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                                    )
                                ) { width -> (width / 12) * direction } +
                                scaleIn(
                                    initialScale = 0.988f,
                                    animationSpec = tween(durationMillis = 360)
                                )
                            ).togetherWith(
                            fadeOut(tween(durationMillis = 170)) +
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 240)
                                ) { width -> (-width / 18) * direction } +
                                scaleOut(
                                    targetScale = 0.994f,
                                    animationSpec = tween(durationMillis = 240)
                                )
                        )
                    },
                    label = "tab-content"
                ) { tab ->
                    tabContent(tab)
                }
            }

            content()
            CustomBottomNav(
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                liquidGlass = settings.liquidGlass,
                visible = navVisible && !navHidden,
                hideFraction = navHideFraction,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        celebration?.let { task ->
            DownloadSavedCard(
                title = task.title,
                onDone = { celebration = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = if (navVisible && !navHidden) 104.dp else 22.dp
                    )
            )
        }

        VidVibeToastHost(
            message = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (navVisible && !navHidden) 124.dp else 26.dp)
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

        AnimatedVisibility(
            visible = playerVideo != null,
            enter = fadeIn(tween(220, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
                ),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200))
        ) {
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
        }

        AnimatedVisibility(
            visible = showAudioPlayer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(360, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
            ) + fadeOut(tween(200))
        ) {
            if (showAudioPlayer) {
                AudioPlayerScreen(onBack = { showAudioPlayer = false })
            }
        }

        val audioState by AudioEngine.state.collectAsStateWithLifecycle()
        if (
            audioState.current != null &&
            !showAudioPlayer &&
            playerVideo == null &&
            !showTour
        ) {
            AudioMiniPlayer(
                onOpen = { showAudioPlayer = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (navVisible && !navHidden) 92.dp else 14.dp)
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
