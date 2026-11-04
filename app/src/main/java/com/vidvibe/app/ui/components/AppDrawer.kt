package com.vidvibe.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.BuildConfig
import com.vidvibe.app.R
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeSelectionBlue
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary

@Composable
fun AppDrawer(
    visible: Boolean,
    selectedTab: Int,
    videoCount: Int,
    playlistCount: Int,
    onDismiss: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onRefresh: () -> Unit,
    onAbout: () -> Unit,
    onOpenGitHub: () -> Unit
) {
    BackHandler(enabled = visible) { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(160)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp),
                color = Color(0xFF0C0D12),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                shadowElevation = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(VidVibeSurface2, RoundedCornerShape(13.dp))
                                .border(1.dp, VidVibeBorder, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "VidVibe",
                                style = MaterialTheme.typography.titleMedium,
                                color = VidVibeText,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Free and open source",
                                style = MaterialTheme.typography.bodySmall,
                                color = VidVibeTextTertiary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(VidVibeSurface2, CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close menu",
                                tint = VidVibeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DrawerStat(
                            value = videoCount.toString(),
                            label = if (videoCount == 1) "video" else "videos",
                            modifier = Modifier.weight(1f)
                        )
                        DrawerStat(
                            value = playlistCount.toString(),
                            label = if (playlistCount == 1) "playlist" else "playlists",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    DrawerSectionLabel("Browse")
                    DrawerItem(
                        icon = Icons.Outlined.Home,
                        title = "Local",
                        subtitle = "Videos on this device",
                        selected = selectedTab == 0,
                        onClick = { onSelectTab(0) }
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Download,
                        title = "Downloader",
                        subtitle = "Save videos from a link",
                        selected = selectedTab == 1,
                        onClick = { onSelectTab(1) }
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Person,
                        title = "Me",
                        subtitle = "Appearance and settings",
                        selected = selectedTab == 2,
                        onClick = { onSelectTab(2) }
                    )

                    Spacer(Modifier.height(20.dp))
                    DrawerSectionLabel("Quick actions")
                    DrawerItem(
                        icon = Icons.Outlined.Refresh,
                        title = "Refresh library",
                        subtitle = "Scan this device again",
                        selected = false,
                        onClick = {
                            onRefresh()
                            onDismiss()
                        }
                    )

                    Spacer(Modifier.height(20.dp))
                    DrawerSectionLabel("About")
                    DrawerItem(
                        icon = Icons.Outlined.Info,
                        title = "About VidVibe",
                        subtitle = "Version ${BuildConfig.VERSION_NAME}",
                        selected = false,
                        onClick = {
                            onAbout()
                            onDismiss()
                        }
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Code,
                        title = "Source code",
                        subtitle = "github.com/xspeen/VidVibe",
                        selected = false,
                        onClick = {
                            onOpenGitHub()
                            onDismiss()
                        }
                    )

                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "MIT License",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = VidVibeTextTertiary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Your videos never leave this device.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = VidVibeTextTertiary
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DrawerStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(VidVibeSurface2, RoundedCornerShape(13.dp))
            .border(1.dp, VidVibeBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = VidVibeText,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = VidVibeTextTertiary
        )
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = VidVibeTextTertiary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(
                if (selected) VidVibeSurface3 else Color.Transparent,
                RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) VidVibeText else VidVibeTextSecondary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeText,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VidVibeTextTertiary,
                maxLines = 1
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(VidVibeSelectionBlue, CircleShape)
            )
        }
    }
}
