package com.vidvibe.app.ui.screens

import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vidvibe.app.data.DeleteResult
import com.vidvibe.app.data.TrashedVideo
import com.vidvibe.app.data.formatDuration
import com.vidvibe.app.data.formatFileSize
import com.vidvibe.app.data.formatVideoDate
import com.vidvibe.app.ui.components.FloatingMenuColor
import com.vidvibe.app.ui.components.FloatingMenuContext
import com.vidvibe.app.ui.components.FloatingMenuState
import com.vidvibe.app.ui.components.FloatingPanel
import com.vidvibe.app.ui.components.VidVibeFloatingMenu
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeGreen
import com.vidvibe.app.ui.theme.VidVibeRedSoft
import com.vidvibe.app.ui.theme.VidVibeSelectionBlue
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.launch

private enum class TrashAction {
    Restore,
    Delete
}

@Composable
fun TrashScreen(
    items: List<TrashedVideo>,
    onBack: () -> Unit,
    onSelectionModeChanged: (Boolean) -> Unit,
    onRestore: (Set<Long>) -> Unit,
    onDeletePermanently: suspend (Set<Long>) -> DeleteResult,
    onLaunchDeleteConfirmation: (IntentSender) -> Unit,
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val menu = remember { FloatingMenuState() }

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingAction by remember { mutableStateOf(TrashAction.Restore) }

    val selectionActive = selectedIds.isNotEmpty()

    LaunchedEffect(selectionActive) {
        onSelectionModeChanged(selectionActive)
    }

    DisposableEffect(Unit) {
        onDispose { onSelectionModeChanged(false) }
    }

    LaunchedEffect(items) {
        val available = items.map { it.id }.toSet()
        if (!available.containsAll(selectedIds)) {
            selectedIds = selectedIds.intersect(available)
            if (selectedIds.isEmpty()) menu.hide()
        }
    }

    fun clearSelection() {
        selectedIds = emptySet()
        menu.hide()
    }

    fun enterSelection(id: Long) {
        selectedIds = selectedIds + id
        pendingAction = TrashAction.Restore
        menu.openActions(FloatingMenuContext.Trash)
        haptics.performHapticFeedback(
            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
        )
    }

    fun toggle(id: Long) {
        val updated = if (id in selectedIds) selectedIds - id else selectedIds + id
        selectedIds = updated
        haptics.performHapticFeedback(
            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
        )
        if (updated.isEmpty()) {
            menu.hide()
        } else if (menu.panel == FloatingPanel.Hidden) {
            menu.openActions(FloatingMenuContext.Trash)
        }
    }

    fun finishRestore() {
        val ids = selectedIds
        if (ids.isEmpty()) {
            menu.hide()
            return
        }
        onRestore(ids)
        clearSelection()
        showToast(if (ids.size == 1) "Video restored" else "${ids.size} videos restored")
    }

    fun finishDelete() {
        val ids = selectedIds
        if (ids.isEmpty()) {
            menu.hide()
            return
        }
        scope.launch {
            when (val result = onDeletePermanently(ids)) {
                DeleteResult.Deleted -> {
                    clearSelection()
                    showToast(
                        if (ids.size == 1) "Video deleted permanently"
                        else "${ids.size} videos deleted permanently"
                    )
                }

                is DeleteResult.NeedsConfirmation -> {
                    menu.hide()
                    onLaunchDeleteConfirmation(result.intentSender)
                    showToast("Confirm deletion in the system dialog")
                }

                is DeleteResult.Failed -> {
                    menu.hide()
                    showToast(result.message)
                }
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            menu.success -> Unit
            menu.panel != FloatingPanel.Hidden -> menu.hide()
            selectionActive -> clearSelection()
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 40.dp,
                                bottomEnd = 40.dp
                            )
                        )
                ) {
                    AsyncImage(
                        model = "https://iili.io/nDKt2KN.jpg",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x660A0A0C), Color(0xD90A0A0C))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                color = LocalAppTextColor.current,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Recently Deleted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = LocalAppTextColor.current,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                EmptyTrash()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 160.dp
                    )
                ) {
                    items(items, key = { it.id }) { item ->
                        TrashRow(
                            item = item,
                            selected = item.id in selectedIds,
                            selectionActive = selectionActive,
                            onClick = {
                                if (selectionActive) toggle(item.id)
                            },
                            onLongClick = {
                                if (selectionActive) toggle(item.id) else enterSelection(item.id)
                            }
                        )
                    }
                }
            }
        }

        VidVibeFloatingMenu(
            state = menu,
            onShare = {},
            onAddTo = {},
            onDelete = {},
            onRestore = {
                pendingAction = TrashAction.Restore
                menu.openSlide(FloatingMenuColor.Restore, "Swipe to Restore")
            },
            onDeletePermanently = {
                pendingAction = TrashAction.Delete
                menu.openSlide(FloatingMenuColor.Delete, "Swipe to Delete")
            },
            onCancel = { clearSelection() },
            onQualityPicked = {},
            onSlideConfirmed = { menu.beginProgress(auto = true) },
            onAutoProgressFinished = {
                when (pendingAction) {
                    TrashAction.Restore -> finishRestore()
                    TrashAction.Delete -> finishDelete()
                }
            },
            onSuccessFinished = { menu.hide() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashRow(
    item: TrashedVideo,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) VidVibeSelectionBlue.copy(alpha = 0.15f)
                else Color(0xFF0A0A0C)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = selectionActive,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) VidVibeSelectionBlue else Color.Transparent
                    )
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

        Box(
            modifier = Modifier
                .width(108.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C21))
        ) {
            AsyncImage(
                model = Uri.parse(item.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        Color.Black.copy(alpha = 0.75f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(item.durationMs),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = LocalAppTextColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatVideoDate(item.dateAddedSeconds)} · ${formatFileSize(item.sizeBytes)}",
                fontSize = 11.sp,
                color = VidVibeTextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyTrash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(VidVibeSurface2)
                    .border(1.dp, VidVibeBorder, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = VidVibeRedSoft,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Trash is empty",
                style = MaterialTheme.typography.titleMedium,
                color = VidVibeText,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Videos you remove from your library appear here. Restore them anytime, or delete them permanently.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(VidVibeSurface2)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = VidVibeGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Restored videos return to your library",
                    style = MaterialTheme.typography.bodySmall,
                    color = VidVibeTextSecondary
                )
            }
        }
    }
}
