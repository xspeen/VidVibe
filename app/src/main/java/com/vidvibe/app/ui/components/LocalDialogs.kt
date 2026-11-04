package com.vidvibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vidvibe.app.data.Playlist
import com.vidvibe.app.ui.theme.VidVibeAccentRed
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.delay

@Composable
fun PlaylistNameDialog(
    visible: Boolean,
    title: String = "New playlist",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!visible) return
    var name by remember(visible) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(visible) {
        delay(120)
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                color = Color(0xFF1E1F28),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                shadowElevation = 24.dp
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                "Enter playlist name",
                                color = VidVibeTextTertiary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (name.isNotBlank()) {
                                    keyboard?.hide()
                                    onSave(name)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VidVibeAccentRed,
                            unfocusedBorderColor = VidVibeBorder,
                            focusedContainerColor = VidVibeSurface2,
                            unfocusedContainerColor = VidVibeSurface2,
                            focusedTextColor = VidVibeText,
                            unfocusedTextColor = VidVibeText,
                            cursorColor = VidVibeAccentRed
                        )
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DialogButton(
                            text = "Cancel",
                            container = VidVibeSurface3,
                            content = VidVibeText,
                            onClick = {
                                keyboard?.hide()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DialogButton(
                            text = "Save",
                            container = VidVibeAccentRed,
                            content = Color.White,
                            enabled = name.isNotBlank(),
                            onClick = {
                                keyboard?.hide()
                                onSave(name)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistPickerDialog(
    visible: Boolean,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                color = Color(0xFF1E1F28),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                shadowElevation = 24.dp
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        text = "Add to playlist",
                        style = MaterialTheme.typography.titleMedium,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists yet. Create one to group these videos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VidVibeTextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            playlists.forEach { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(playlist) }
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlaylistPlay,
                                        contentDescription = null,
                                        tint = VidVibeTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = VidVibeText
                                        )
                                        Text(
                                            text = "${playlist.videoIds.size} videos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VidVibeTextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCreateNew() }
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = VidVibeAccentRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "New playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VidVibeAccentRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = VidVibeTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                if (enabled) container else container.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) content else content.copy(alpha = 0.45f)
        )
    }
}
