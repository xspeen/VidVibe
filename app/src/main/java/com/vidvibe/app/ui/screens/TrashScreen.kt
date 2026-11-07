package com.vidvibe.app.ui.screens

import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.vidvibe.app.ui.components.DeleteSlideSheet
import com.vidvibe.app.ui.components.RoundIconButton
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeGreen
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeRedSoft
import com.vidvibe.app.ui.theme.VidVibeSelectionBlue
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary

@Composable
fun TrashScreen(
    items: List<TrashedVideo>,
    onBack: () -> Unit,
    onRestore: (Set<Long>) -> Unit,
    onDeletePermanently: suspend (Set<Long>) -> DeleteResult,
    onLaunchDeleteConfirmation: (IntentSender) -> Unit,
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current
    val haptics = LocalHapticFeedback.current

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    var deleteStatusMessage by remember { mutableStateOf("") }

    val selectionActive = selectedIds.isNotEmpty()

    LaunchedEffect(items) {
        val available = items.map { it.id }.toSet()
        if (!available.containsAll(selectedIds)) {
            selectedIds = selectedIds.intersect(available)
        }
    }

    fun clearSelection() {
        selectedIds = emptySet()
        showDeleteSheet = false
    }

    fun toggle(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        haptics.performHapticFeedback(
            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
        )
    }

    BackHandler(enabled = true) {
        if (selectionActive) clearSelection() else onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 40.dp,
                            bottomEnd = 40.dp
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE61C1C21), Color(0xF20A0A0C))
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(
                            bottomStart = 40.dp,
                            bottomEnd = 40.dp
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectionActive) {
                        RoundIconButton(
                            onClick = { clearSelection() },
                            icon = Icons.Outlined.Check,
                            contentDescription = "Done selecting",
                            tint = VidVibeText
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = VidVibeText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        RoundIconButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == items.size) {
                                    emptySet()
                                } else {
                                    items.map { it.id }.toSet()
                                }
                            },
                            icon = Icons.Outlined.SelectAll,
                            contentDescription = "Select all",
                            tint = VidVibeText
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
                            color = VidVibeText,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!selectionActive) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Trash",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = VidVibeText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${items.size} ${if (items.size == 1) "video" else "videos"} · ${formatFileSize(items.sumOf { it.sizeBytes })}",
                            fontSize = 11.sp,
                            color = VidVibeTextTertiary,
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
                        bottom = if (selectionActive) 104.dp else 160.dp
                    )
                ) {
                    items(items, key = { it.id }) { item ->
                        TrashRow(
                            item = item,
                            selected = item.id in selectedIds,
                            selectionActive = selectionActive,
                            onClick = {
                                if (selectionActive) toggle(item.id) else {
                                    selectedIds = setOf(item.id)
                                }
                            },
                            onLongClick = { toggle(item.id) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectionActive,
            enter = slideInVertically(
                initialOffsetY = { it + 40 },
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 430f)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it + 40 },
                animationSpec = tween(220)
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 22.dp)
        ) {
            Surface(
                color = Color(0xFF22232F),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                shadowElevation = 18.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrashAction(
                        icon = Icons.Outlined.Restore,
                        label = "Restore",
                        tint = VidVibeGreen,
                        onClick = {
                            val ids = selectedIds
                            onRestore(ids)
                            clearSelection()
                            showToast(
                                if (ids.size == 1) "Video restored"
                                else "${ids.size} videos restored"
                            )
                        }
                    )
                    TrashAction(
                        icon = Icons.Outlined.DeleteForever,
                        label = "Delete",
                        tint = VidVibeRed,
                        onClick = { showDeleteSheet = true }
                    )
                }
            }
        }

        DeleteSlideSheet(
            visible = showDeleteSheet,
            selectedCount = selectedIds.size,
            onDismiss = { showDeleteSheet = false },
            onConfirm = {
                val ids = selectedIds
                if (ids.isEmpty()) {
                    false
                } else {
                    when (val result = onDeletePermanently(ids)) {
                        DeleteResult.Deleted -> {
                            deleteStatusMessage = if (ids.size == 1) {
                                "Video deleted permanently"
                            } else {
                                "${ids.size} videos deleted permanently"
                            }
                            true
                        }
                        is DeleteResult.NeedsConfirmation -> {
                            onLaunchDeleteConfirmation(result.intentSender)
                            showDeleteSheet = false
                            showToast("Confirm deletion in the system dialog")
                            false
                        }
                        is DeleteResult.Failed -> {
                            showToast(result.message)
                            false
                        }
                    }
                }
            },
            onConfirmed = {
                showDeleteSheet = false
                clearSelection()
                showToast(deleteStatusMessage.ifBlank { "Deleted permanently" })
            },
            title = "Delete ${selectedIds.size} permanently?",
            description = "These files will be removed from this device. This cannot be undone.",
            slideLabel = "Slide to Delete ${selectedIds.size} ${
                if (selectedIds.size == 1) "file" else "files"
            }"
        )
    }
}

@Composable
private fun RestoreAllButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VidVibeSurface2)
            .border(1.dp, VidVibeBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Restore,
            contentDescription = null,
            tint = VidVibeGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Restore all",
            style = MaterialTheme.typography.labelMedium,
            color = VidVibeTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TrashAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = VidVibeText,
            fontWeight = FontWeight.Medium
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
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) VidVibeSelectionBlue.copy(alpha = 0.12f) else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = selectionActive,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) VidVibeSelectionBlue else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (selected) VidVibeSelectionBlue else VidVibeTextTertiary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFF050507),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(108.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF181920))
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
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = VidVibeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${formatVideoDate(item.dateAddedSeconds)} · ${formatFileSize(item.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                    .background(VidVibeSurface3)
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
