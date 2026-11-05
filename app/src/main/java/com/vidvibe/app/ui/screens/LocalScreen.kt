package com.vidvibe.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vidvibe.app.data.Playlist
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.ui.components.ClassicLoader
import com.vidvibe.app.ui.components.DeleteSlideSheet
import com.vidvibe.app.ui.components.PlaylistNameDialog
import com.vidvibe.app.ui.components.PlaylistPickerDialog
import com.vidvibe.app.ui.components.PullToRefresh
import com.vidvibe.app.ui.components.RoundIconButton
import com.vidvibe.app.ui.components.SelectionMenu
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSelectionBlue
import com.vidvibe.app.ui.theme.VidVibeSurface
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LocalScreen(
    videos: List<VideoItem>,
    isLoading: Boolean,
    playlists: List<Playlist>,
    onOpenVideo: (VideoItem) -> Unit,
    onRefresh: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSelectionModeChanged: (Boolean) -> Unit,
    onMoveToTrash: (List<VideoItem>) -> Unit,
    onCreatePlaylist: (String) -> Playlist?,
    onAddVideosToPlaylist: (String, Set<Long>) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 72.dp.toPx() }

    var currentTab by rememberSaveable { mutableStateOf(0) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isGridView by rememberSaveable { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    var deleteStatusMessage by remember { mutableStateOf("") }

    var folderFilter by remember { mutableStateOf<String?>(null) }
    var playlistFilterId by remember { mutableStateOf<String?>(null) }
    var shuffledIds by remember { mutableStateOf<List<Long>?>(null) }

    var showCreatePlaylist by remember { mutableStateOf(false) }
    var addAfterCreate by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var playlistPendingDelete by remember { mutableStateOf<Playlist?>(null) }

    val activePlaylist = playlists.firstOrNull { it.id == playlistFilterId }

    LaunchedEffect(playlists, playlistFilterId) {
        if (playlistFilterId != null && playlists.none { it.id == playlistFilterId }) {
            playlistFilterId = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { onSelectionModeChanged(false) }
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
        showDeleteSheet = false
        onSelectionModeChanged(false)
    }

    fun enterSelection(videoId: Long) {
        selectionMode = true
        selectedIds = selectedIds + videoId
        onSelectionModeChanged(true)
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    fun toggleSelection(videoId: Long) {
        val updated = if (videoId in selectedIds) selectedIds - videoId else selectedIds + videoId
        selectedIds = updated
        if (updated.isEmpty()) {
            exitSelection()
        }
    }

    fun closeSearch() {
        searchOpen = false
        searchQuery = ""
        keyboardController?.hide()
    }

    fun selectTab(index: Int) {
        if (index == currentTab) return
        currentTab = index
        if (index != 0) {
            folderFilter = null
            playlistFilterId = null
        }
    }

    val visibleVideos = remember(
        videos,
        searchQuery,
        folderFilter,
        playlistFilterId,
        playlists,
        shuffledIds
    ) {
        var filtered = videos
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.displayName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
        folderFilter?.let { folder ->
            filtered = filtered.filter { it.bucketName == folder }
        }
        if (playlistFilterId != null) {
            val playlist = playlists.firstOrNull { it.id == playlistFilterId }
            if (playlist != null) {
                filtered = filtered.filter { it.id in playlist.videoIds }
            }
        }
        if (shuffledIds != null) {
            val order = shuffledIds!!.withIndex().associate { it.value to it.index }
            filtered = filtered.sortedBy { order[it.id] ?: Int.MAX_VALUE }
        }
        filtered
    }

    val selectedVideos = videos.filter { it.id in selectedIds }
    val filterLabel = folderFilter?.let { "Folder · $it" }
        ?: activePlaylist?.let { "Playlist · ${it.name}" }

    BackHandler(enabled = selectionMode || searchOpen) {
        when {
            selectionMode -> exitSelection()
            searchOpen -> closeSearch()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        LocalTopBar(
            selectionMode = selectionMode,
            selectedCount = selectedIds.size,
            searchOpen = searchOpen,
            searchQuery = searchQuery,
            onSearchQueryChange = {
                searchQuery = it
                currentTab = 0
            },
            onOpenSearch = { searchOpen = true },
            onCloseSearch = { closeSearch() },
            onExitSelection = { exitSelection() },
            onSelectAll = {
                if (visibleVideos.isEmpty()) return@LocalTopBar
                if (selectedIds.size == visibleVideos.size) {
                    exitSelection()
                } else {
                    selectionMode = true
                    selectedIds = visibleVideos.map { it.id }.toSet()
                    onSelectionModeChanged(true)
                }
            },
            currentTab = currentTab,
            isGridView = isGridView,
            onShuffle = {
                if (videos.size > 1) {
                    shuffledIds = videos.map { it.id }.shuffled()
                    showToast("Shuffled")
                }
            },
            onToggleView = { isGridView = !isGridView },
            onCreatePlaylist = {
                addAfterCreate = false
                showCreatePlaylist = true
            },
            onOpenDrawer = onOpenDrawer
        )

        LocalTabBar(
            selectedTab = currentTab,
            onSelect = { selectTab(it) },
            dimmed = selectionMode
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(selectionMode, searchOpen, currentTab) {
                    if (selectionMode || searchOpen) return@pointerInput
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            when {
                                dragTotal <= -swipeThreshold && currentTab < 2 ->
                                    selectTab(currentTab + 1)
                                dragTotal >= swipeThreshold && currentTab > 0 ->
                                    selectTab(currentTab - 1)
                            }
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
        ) {
            PullToRefresh(
                onRefresh = {
                    shuffledIds = null
                    onRefresh()
                }
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(180))) togetherWith
                                (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(160)))
                        } else {
                            (slideInHorizontally(tween(260)) { -it / 4 } + fadeIn(tween(180))) togetherWith
                                (slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(160)))
                        }
                    },
                    label = "local-tab"
                ) { tab ->
                    when (tab) {
                        0 -> VideosPane(
                            videos = visibleVideos,
                            isLoading = isLoading,
                            isSelectionMode = selectionMode,
                            selectedIds = selectedIds,
                            isGridView = isGridView,
                            filterLabel = filterLabel,
                            hasSearch = searchQuery.isNotBlank(),
                            onClearFilter = {
                                folderFilter = null
                                playlistFilterId = null
                            },
                            onClick = { video ->
                                if (selectionMode) toggleSelection(video.id) else onOpenVideo(video)
                            },
                            onLongClick = { video -> enterSelection(video.id) },
                            onMoreClick = { video -> enterSelection(video.id) }
                        )
                        1 -> FoldersPane(
                            videos = videos,
                            onOpenFolder = { folder ->
                                folderFilter = folder
                                playlistFilterId = null
                                currentTab = 0
                            }
                        )
                        2 -> PlaylistsPane(
                            playlists = playlists,
                            onCreatePlaylist = {
                                addAfterCreate = false
                                showCreatePlaylist = true
                            },
                            onOpenPlaylist = { playlist ->
                                playlistFilterId = playlist.id
                                folderFilter = null
                                currentTab = 0
                            },
                            onDeletePlaylist = { playlistPendingDelete = it }
                        )
                    }
                }
            }

            SelectionMenu(
                visible = selectionMode && !showDeleteSheet && selectedIds.isNotEmpty(),
                onShare = {
                    shareVideos(context, selectedVideos) { showToast(it) }
                },
                onAddTo = { showPlaylistPicker = true },
                onDelete = { showDeleteSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 22.dp)
            )

            DeleteSlideSheet(
                visible = showDeleteSheet,
                selectedCount = selectedIds.size,
                onDismiss = { showDeleteSheet = false },
                onConfirm = {
                    val items = videos.filter { it.id in selectedIds }
                    if (items.isEmpty()) {
                        false
                    } else {
                        onMoveToTrash(items)
                        deleteStatusMessage = if (items.size == 1) {
                            "Moved to Trash"
                        } else {
                            "${items.size} videos moved to Trash"
                        }
                        true
                    }
                },
                onConfirmed = {
                    showDeleteSheet = false
                    val count = selectedIds.size
                    exitSelection()
                    showToast(
                        deleteStatusMessage.ifBlank {
                            if (count == 1) "Moved to Trash"
                            else "$count videos moved to Trash"
                        }
                    )
                },
                title = "Move ${selectedIds.size} to Trash?",
                description = "Files in Trash haven't been permanently deleted from your device.",
                slideLabel = "Slide to Delete ${selectedIds.size} ${
                    if (selectedIds.size == 1) "file" else "files"
                }"
            )
        }
    }

    PlaylistNameDialog(
        visible = showCreatePlaylist,
        onDismiss = {
            showCreatePlaylist = false
            addAfterCreate = false
        },
        onSave = { name ->
            val playlist = onCreatePlaylist(name)
            showCreatePlaylist = false
            if (playlist == null) {
                showToast("Enter a playlist name")
            } else if (addAfterCreate) {
                onAddVideosToPlaylist(playlist.id, selectedIds)
                showToast("Added to ${playlist.name}")
                exitSelection()
            } else {
                showToast("Playlist created")
            }
            addAfterCreate = false
        }
    )

    PlaylistPickerDialog(
        visible = showPlaylistPicker,
        playlists = playlists,
        onDismiss = { showPlaylistPicker = false },
        onSelect = { playlist ->
            onAddVideosToPlaylist(playlist.id, selectedIds)
            showPlaylistPicker = false
            showToast("Added to ${playlist.name}")
            exitSelection()
        },
        onCreateNew = {
            showPlaylistPicker = false
            addAfterCreate = true
            showCreatePlaylist = true
        }
    )

    playlistPendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistPendingDelete = null },
            containerColor = VidVibeSurface2,
            titleContentColor = VidVibeText,
            textContentColor = VidVibeTextSecondary,
            title = { Text("Delete playlist?", fontWeight = FontWeight.Medium) },
            text = {
                Text("This only removes the playlist. Your video files stay on your device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(playlist.id)
                        if (playlistFilterId == playlist.id) playlistFilterId = null
                        playlistPendingDelete = null
                        showToast("Playlist deleted")
                    }
                ) {
                    Text("Delete", color = VidVibeRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistPendingDelete = null }) {
                    Text("Cancel", color = VidVibeTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LocalTopBar(
    selectionMode: Boolean,
    selectedCount: Int,
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    currentTab: Int,
    isGridView: Boolean,
    onShuffle: () -> Unit,
    onToggleView: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val background = LocalAppBackground.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            kotlinx.coroutines.delay(90)
            focusRequester.requestFocus()
        } else {
            keyboard?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                RoundIconButton(
                    onClick = onExitSelection,
                    icon = Icons.Outlined.Close,
                    contentDescription = "Exit selection",
                    tint = VidVibeText
                )
                Spacer(Modifier.width(2.dp))
            } else {
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = if (selectionMode) "$selectedCount selected" else "Local",
                style = MaterialTheme.typography.titleLarge,
                color = VidVibeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (selectionMode) {
                RoundIconButton(
                    onClick = onSelectAll,
                    icon = Icons.Outlined.SelectAll,
                    contentDescription = "Select all",
                    tint = VidVibeText
                )
            } else {
                if (currentTab == 0) {
                    RoundIconButton(
                        onClick = onShuffle,
                        icon = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle videos"
                    )
                }
                RoundIconButton(
                    onClick = onOpenSearch,
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search"
                )
                RoundIconButton(
                    onClick = onToggleView,
                    icon = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = "Change layout"
                )
                RoundIconButton(
                    onClick = onCreatePlaylist,
                    icon = Icons.Outlined.PlaylistAdd,
                    contentDescription = "Create playlist"
                )
                RoundIconButton(
                    onClick = onOpenDrawer,
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "Open menu"
                )
            }
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 4 },
            exit = fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 4 },
            modifier = Modifier.matchParentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = VidVibeTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = VidVibeText,
                        fontSize = 16.sp,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                    ),
                    cursorBrush = SolidColor(VidVibeText),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onCloseSearch() }),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search videos...",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = VidVibeTextTertiary
                                )
                            }
                            inner()
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VidVibeSurface3)
                        .clickable(onClick = onCloseSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close search",
                        tint = VidVibeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalTabBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    dimmed: Boolean
) {
    val labels = listOf("Videos", "Folders", "Playlists")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .drawBehind {
                drawLine(
                    color = VidVibeBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .alpha(if (dimmed) 0f else 1f)
    ) {
        val tabWidth = maxWidth / labels.size.toFloat()
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab.toFloat(),
            animationSpec = tween(300),
            label = "tab-ink"
        )

        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = !dimmed) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = if (selectedTab == index) VidVibeText else VidVibeTextTertiary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(2.dp)
                .background(
                    VidVibeText,
                    RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                )
        )
    }
}

@Composable
private fun VideosPane(
    videos: List<VideoItem>,
    isLoading: Boolean,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    isGridView: Boolean,
    filterLabel: String?,
    hasSearch: Boolean,
    onClearFilter: () -> Unit,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit,
    onMoreClick: (VideoItem) -> Unit
) {
    if (isLoading && videos.isEmpty()) {
        LoadingPane()
        return
    }
    if (videos.isEmpty()) {
        EmptyPane(
            icon = Icons.Outlined.VideoLibrary,
            title = if (hasSearch) "No videos match your search" else "No videos yet",
            subtitle = if (hasSearch) {
                "Try a different name or clear the search."
            } else {
                "Videos on this device and downloads will appear here."
            }
        )
        return
    }

    if (isGridView) {
        VideosGrid(
            videos = videos,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            filterLabel = filterLabel,
            onClearFilter = onClearFilter,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        VideosList(
            videos = videos,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            filterLabel = filterLabel,
            onClearFilter = onClearFilter,
            onClick = onClick,
            onLongClick = onLongClick,
            onMoreClick = onMoreClick
        )
    }
}

@Composable
private fun VideosList(
    videos: List<VideoItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    filterLabel: String?,
    onClearFilter: () -> Unit,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit,
    onMoreClick: (VideoItem) -> Unit
) {
    val listState = rememberLazyListState()
    val grouped = remember(videos) {
        videos.groupBy { it.dateLabel }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filterLabel != null) {
                item(key = "active-filter") {
                    ActiveFilterRow(label = filterLabel, onClear = onClearFilter)
                }
            }
            grouped.forEach { (date, dateVideos) ->
                item(key = "date-$date") {
                    DateHeader(date)
                }
                items(
                    items = dateVideos,
                    key = { it.id }
                ) { video ->
                    VideoRow(
                        video = video,
                        isSelectionMode = isSelectionMode,
                        selected = video.id in selectedIds,
                        onClick = { onClick(video) },
                        onLongClick = { onLongClick(video) },
                        onMoreClick = { onMoreClick(video) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun VideosGrid(
    videos: List<VideoItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    filterLabel: String?,
    onClearFilter: () -> Unit,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit
) {
    val grouped = remember(videos) { videos.groupBy { it.dateLabel } }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 960.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filterLabel != null) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "active-filter-grid") {
                    ActiveFilterRow(label = filterLabel, onClear = onClearFilter)
                }
            }
            grouped.forEach { (date, dateVideos) ->
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "grid-date-$date"
                ) {
                    DateHeader(date, modifier = Modifier.padding(start = 4.dp))
                }
                items(
                    items = dateVideos,
                    key = { it.id }
                ) { video ->
                    VideoGridCard(
                        video = video,
                        isSelectionMode = isSelectionMode,
                        selected = video.id in selectedIds,
                        onClick = { onClick(video) },
                        onLongClick = { onLongClick(video) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = date,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        color = VidVibeTextTertiary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoRow(
    video: VideoItem,
    isSelectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 720f),
        label = "video-row-scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) VidVibeSelectionBlue.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(180),
        label = "video-row-background"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f)
        ) {
            SelectionCircle(
                selected = selected,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(108.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF181920))
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            DurationChip(
                duration = video.durationLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = video.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = VidVibeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = video.dateLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = VidVibeTextTertiary,
                maxLines = 1
            )
        }

        if (!isSelectionMode) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Select video",
                    tint = VidVibeTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridCard(
    video: VideoItem,
    isSelectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 720f),
        label = "video-grid-scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) VidVibeSelectionBlue.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(180),
        label = "video-grid-background"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF181920))
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            DurationChip(
                duration = video.durationLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
            )
            if (isSelectionMode) {
                SelectionCircle(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = video.displayName,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = VidVibeText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = video.dateLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = VidVibeTextTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) VidVibeSelectionBlue else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (selected) VidVibeSelectionBlue else VidVibeTextTertiary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DurationChip(
    duration: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(2.dp)
    ) {
        Text(
            text = duration,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ActiveFilterRow(
    label: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = VidVibeSurface2,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = VidVibeTextSecondary
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear filter",
                    tint = VidVibeTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun FoldersPane(
    videos: List<VideoItem>,
    onOpenFolder: (String) -> Unit
) {
    val folders = remember(videos) {
        videos.groupBy { it.bucketName }
            .map { (name, items) -> name to items.size }
            .sortedBy { it.first.lowercase(Locale.US) }
    }

    if (folders.isEmpty()) {
        EmptyPane(
            icon = Icons.Outlined.FolderOpen,
            title = "No folders found",
            subtitle = "Folders appear when videos are available on this device."
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            items(folders, key = { it.first }) { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFolder(name) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VidVibeSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = VidVibeTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = VidVibeText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$count ${if (count == 1) "item" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VidVibeTextTertiary
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = VidVibeTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsPane(
    playlists: List<Playlist>,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item(key = "create-playlist") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreatePlaylist)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VidVibeSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = VidVibeTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Create your playlist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VidVibeText
                    )
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlaylist(playlist) }
                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VidVibeSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistPlay,
                            contentDescription = null,
                            tint = VidVibeTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = VidVibeText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.videoIds.size} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = VidVibeTextTertiary
                        )
                    }
                    IconButton(
                        onClick = { onDeletePlaylist(playlist) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete playlist",
                            tint = VidVibeTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPane() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ClassicLoader(modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Loading videos...",
                style = MaterialTheme.typography.bodySmall,
                color = VidVibeTextTertiary
            )
        }
    }
}

@Composable
private fun EmptyPane(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillParentMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VidVibeTextTertiary.copy(alpha = 0.55f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = VidVibeTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VidVibeTextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                }
            }
        }
    }
}

private fun shareVideos(
    context: android.content.Context,
    videos: List<VideoItem>,
    onError: (String) -> Unit
) {
    if (videos.isEmpty()) {
        onError("Select at least one video to share.")
        return
    }
    val uris = videos.map { it.uri }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            clipData = ClipData.newRawUri("video", uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            val clip = ClipData.newRawUri("videos", uris.first())
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            clipData = clip
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, "Share videos"))
    } catch (_: ActivityNotFoundException) {
        onError("No app can share this video.")
    }
}
