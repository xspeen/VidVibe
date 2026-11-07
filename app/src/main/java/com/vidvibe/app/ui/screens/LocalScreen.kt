package com.vidvibe.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.font.FontFamily
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
import com.vidvibe.app.ui.components.FluidBackground
import com.vidvibe.app.ui.components.PlaylistNameDialog
import com.vidvibe.app.ui.components.PlaylistPickerDialog
import com.vidvibe.app.ui.components.PullToRefresh
import com.vidvibe.app.ui.components.RoundIconButton
import com.vidvibe.app.ui.components.SelectionMenu
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.launch
import java.util.Locale

// Palette copied from the supplied prototype.
private val Ink = Color(0xFF0A0A0C)
private val GlassSurface = Color(0xFF1C1C21)
private val GlassText = Color(0xFFF5F5F7)
private val GlassText2 = Color(0xFFA0A0A8)
private val GlassText3 = Color(0xFF6B6B75)
private val GlassBlue = Color(0xFF4E9BFF)
private val GlassBorder = Color.White.copy(alpha = 0.10f)

@Composable
fun LocalScreen(
    videos: List<VideoItem>,
    isLoading: Boolean,
    playlists: List<Playlist>,
    onOpenVideo: (List<VideoItem>, Int) -> Unit,
    onRefresh: () -> Unit,
    onSelectionModeChanged: (Boolean) -> Unit,
    onMoveToTrash: (List<VideoItem>) -> Unit,
    onCreatePlaylist: (String) -> Playlist?,
    onAddVideosToPlaylist: (String, Set<Long>) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    showToast: (String) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 72.dp.toPx() }

    var currentTab by rememberSaveable { mutableStateOf(0) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        FluidBackground()

        Column(Modifier.fillMaxSize()) {
            LocalHero(
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
                    if (visibleVideos.isEmpty()) return@LocalHero
                    if (selectedIds.size == visibleVideos.size) {
                        exitSelection()
                    } else {
                        selectionMode = true
                        selectedIds = visibleVideos.map { it.id }.toSet()
                        onSelectionModeChanged(true)
                    }
                },
                onShuffle = {
                    if (videos.size > 1) {
                        shuffledIds = videos.map { it.id }.shuffled()
                        showToast("Shuffled")
                    }
                }
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
                                filterLabel = filterLabel,
                                hasSearch = searchQuery.isNotBlank(),
                                onClearFilter = {
                                    folderFilter = null
                                    playlistFilterId = null
                                },
                                onClick = { video ->
                                    if (selectionMode) {
                                        toggleSelection(video.id)
                                    } else {
                                        val index = visibleVideos.indexOfFirst { it.id == video.id }
                                        onOpenVideo(visibleVideos, index.coerceAtLeast(0))
                                    }
                                },
                                onLongClick = { video -> enterSelection(video.id) }
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
private fun LocalHero(
    selectionMode: Boolean,
    selectedCount: Int,
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onShuffle: () -> Unit
) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE61C1C21), Color(0xF20A0A0C))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.06f))
                ),
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                RoundIconButton(
                    onClick = onExitSelection,
                    icon = Icons.Outlined.Close,
                    contentDescription = "Exit selection",
                    tint = GlassText
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$selectedCount selected",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                RoundIconButton(
                    onClick = onSelectAll,
                    icon = Icons.Outlined.SelectAll,
                    contentDescription = "Select all",
                    tint = GlassText
                )
            } else {
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
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                        .clickable(onClick = onShuffle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle videos",
                        tint = GlassText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = searchOpen,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.95f)) togetherWith
                    (fadeOut(tween(180)) + scaleOut(targetScale = 0.95f))
            },
            label = "search-pill"
        ) { open ->
            if (open) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(26.dp))
                        .padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = GlassText2,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onCloseSearch() }),
                        decorationBox = { inner ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search...",
                                        fontSize = 15.sp,
                                        color = GlassText3
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable(onClick = onCloseSearch),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close search",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(26.dp))
                        .clickable(onClick = onOpenSearch)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = GlassText3,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Search videos or folders...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassText3
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
            .alpha(if (dimmed) 0f else 1f)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x99141419))
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / labels.size.toFloat()
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedTab.toFloat(),
                animationSpec = tween(
                    durationMillis = 350,
                    easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                ),
                label = "tab-glass"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(18.dp)
                    )
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
                            fontSize = 13.sp,
                            color = if (selectedTab == index) GlassText else GlassText3,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold
                            else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideosPane(
    videos: List<VideoItem>,
    isLoading: Boolean,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    filterLabel: String?,
    hasSearch: Boolean,
    onClearFilter: () -> Unit,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit
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

    VideosList(
        videos = videos,
        isSelectionMode = isSelectionMode,
        selectedIds = selectedIds,
        filterLabel = filterLabel,
        onClearFilter = onClearFilter,
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideosList(
    videos: List<VideoItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    filterLabel: String?,
    onClearFilter: () -> Unit,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit
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
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            if (filterLabel != null) {
                item(key = "active-filter") {
                    ActiveFilterRow(label = filterLabel, onClear = onClearFilter)
                }
            }
            grouped.forEach { (date, dateVideos) ->
                item(key = "date-$date") {
                    DateLabel(date)
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
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun DateLabel(date: String) {
    Text(
        text = date,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = GlassText3,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
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
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "video-row-scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) GlassBlue.copy(alpha = 0.15f) else Ink,
        animationSpec = tween(180),
        label = "video-row-background"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f)
        ) {
            SelectionCircle(
                selected = selected,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(108.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GlassSurface)
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

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = video.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GlassText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = video.dateLabel,
                fontSize = 11.sp,
                color = GlassText3,
                maxLines = 1
            )
        }
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
            .background(if (selected) GlassBlue else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (selected) GlassBlue else GlassText3,
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
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = duration,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            color = GlassSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassText2
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear filter",
                    tint = GlassText2,
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
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp)
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
                            .background(GlassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = GlassText2,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$count ${if (count == 1) "item" else "items"}",
                            fontSize = 12.sp,
                            color = GlassText3
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = GlassText3,
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
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp)
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
                            .background(GlassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = GlassText2,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Create your playlist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassText
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
                            .background(GlassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistPlay,
                            contentDescription = null,
                            tint = GlassText2,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.videoIds.size} videos",
                            fontSize = 12.sp,
                            color = GlassText3
                        )
                    }
                    IconButton(
                        onClick = { onDeletePlaylist(playlist) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete playlist",
                            tint = GlassText3,
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
                fontSize = 12.sp,
                color = GlassText3
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
                        tint = GlassText3.copy(alpha = 0.55f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassText2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = GlassText3,
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
