package com.vidvibe.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.BuildConfig
import com.vidvibe.app.R
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.ui.components.AboutVidVibeDialog
import com.vidvibe.app.ui.components.RoundIconButton
import com.vidvibe.app.ui.components.VIDVIBE_GITHUB_URL
import com.vidvibe.app.ui.theme.BackgroundOptions
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.VidVibeAccentRed
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlin.math.roundToInt

@Composable
fun MeScreen(
    settings: AppSettings,
    videoCount: Int,
    playlistCount: Int,
    onFontSizeChange: (Float) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onBackgroundChange: (Int) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    showToast: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }
    var showDownloadSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showDownloadSettings) { showDownloadSettings = false }

    AnimatedVisibility(
        visible = showDownloadSettings,
        enter = slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(180)),
        exit = slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(150))
    ) {
        DownloadSettingsPage(
            wifiOnly = settings.wifiOnly,
            onWifiOnlyChange = onWifiOnlyChange,
            onBack = { showDownloadSettings = false }
        )
    }

    if (!showDownloadSettings) {
        MeHome(
            settings = settings,
            videoCount = videoCount,
            playlistCount = playlistCount,
            onFontSizeChange = onFontSizeChange,
            onLiquidGlassChange = onLiquidGlassChange,
            onBackgroundChange = onBackgroundChange,
            onWifiOnlyChange = onWifiOnlyChange,
            onOpenDownloadSettings = { showDownloadSettings = true },
            onOpenAbout = { showAbout = true },
            onOpenLicense = { showLicense = true },
            onOpenGitHub = {
                openUrl(context, VIDVIBE_GITHUB_URL) { showToast(it) }
            }
        )
    }

    if (showAbout) {
        AboutVidVibeDialog(
            onDismiss = { showAbout = false },
            onOpenGitHub = {
                showAbout = false
                openUrl(context, VIDVIBE_GITHUB_URL) { showToast(it) }
            }
        )
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            containerColor = VidVibeSurface2,
            titleContentColor = VidVibeText,
            textContentColor = VidVibeTextSecondary,
            title = { Text("MIT License", fontWeight = FontWeight.Medium) },
            text = {
                Text(
                    "Copyright (c) 2026 VidVibe\n\n" +
                        "Permission is hereby granted, free of charge, to any person " +
                        "obtaining a copy of this software and associated documentation " +
                        "files, to deal in the Software without restriction, including " +
                        "without limitation the rights to use, copy, modify, merge, " +
                        "publish, distribute, sublicense, and/or sell copies of the " +
                        "Software, subject to the conditions of the MIT License.\n\n" +
                        "The Software is provided without warranty of any kind."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) {
                    Text("Close", color = VidVibeAccentRed)
                }
            }
        )
    }
}

@Composable
private fun MeHome(
    settings: AppSettings,
    videoCount: Int,
    playlistCount: Int,
    onFontSizeChange: (Float) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onBackgroundChange: (Int) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onOpenDownloadSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenGitHub: () -> Unit
) {
    val background = LocalAppBackground.current
    var fontValue by rememberSaveable { mutableFloatStateOf(settings.fontSizeSp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Me",
                    style = MaterialTheme.typography.titleLarge,
                    color = VidVibeText,
                    modifier = Modifier.weight(1f)
                )
                RoundIconButton(
                    onClick = onOpenAbout,
                    icon = Icons.Outlined.Info,
                    contentDescription = "About VidVibe"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                ProfileCard(
                    videoCount = videoCount,
                    playlistCount = playlistCount
                )

                Spacer(Modifier.height(22.dp))
                SectionLabel("Appearance")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
                ) {
                    Column {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = null,
                                    tint = VidVibeTextSecondary,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Font size",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = VidVibeText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Applies to the whole app",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VidVibeTextTertiary
                                    )
                                }
                                Surface(
                                    color = VidVibeSurface3,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${fontValue.roundToInt()}sp",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VidVibeTextSecondary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Slider(
                                value = fontValue,
                                onValueChange = {
                                    fontValue = it
                                    onFontSizeChange(it)
                                },
                                valueRange = 12f..24f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = VidVibeAccentRed,
                                    activeTrackColor = VidVibeAccentRed,
                                    inactiveTrackColor = VidVibeSurface3
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "12sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VidVibeTextTertiary
                                )
                                Text(
                                    text = "24sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VidVibeTextTertiary
                                )
                            }
                        }

                        HorizontalDivider(color = VidVibeBorder)

                        SettingRow(
                            icon = Icons.Outlined.Settings,
                            title = "Liquid glass navigation",
                            subtitle = "Translucent blur-style bottom bar",
                            trailing = {
                                Switch(
                                    checked = settings.liquidGlass,
                                    onCheckedChange = onLiquidGlassChange,
                                    colors = vidVibeSwitchColors()
                                )
                            }
                        )

                        HorizontalDivider(color = VidVibeBorder)

                        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = null,
                                    tint = VidVibeTextSecondary,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "App background",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = VidVibeText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = BackgroundOptions.firstOrNull {
                                            it.color.toArgb() == settings.backgroundColor
                                        }?.name ?: "Custom",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VidVibeTextTertiary
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                BackgroundOptions.forEach { option ->
                                    val selected = option.color.toArgb() == settings.backgroundColor
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(option.color)
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) VidVibeText
                                                else Color.White.copy(alpha = 0.22f),
                                                shape = CircleShape
                                            )
                                            .clickable { onBackgroundChange(option.color.toArgb()) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Downloads")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
                ) {
                    Column {
                        SettingRow(
                            icon = Icons.Outlined.Wifi,
                            title = "Wi-Fi only",
                            subtitle = "Block downloads on mobile data",
                            trailing = {
                                Switch(
                                    checked = settings.wifiOnly,
                                    onCheckedChange = onWifiOnlyChange,
                                    colors = vidVibeSwitchColors()
                                )
                            }
                        )
                        HorizontalDivider(color = VidVibeBorder)
                        SettingRow(
                            icon = Icons.Outlined.Download,
                            title = "Download settings",
                            subtitle = "Quality, storage and connections",
                            onClick = onOpenDownloadSettings,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = VidVibeTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("About")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
                ) {
                    Column {
                        SettingRow(
                            icon = Icons.Outlined.Info,
                            title = "About VidVibe",
                            subtitle = "Version ${BuildConfig.VERSION_NAME}",
                            onClick = onOpenAbout
                        )
                        HorizontalDivider(color = VidVibeBorder)
                        SettingRow(
                            icon = Icons.Outlined.Code,
                            title = "Source code",
                            subtitle = "github.com/xspeen/VidVibe",
                            onClick = onOpenGitHub
                        )
                        HorizontalDivider(color = VidVibeBorder)
                        SettingRow(
                            icon = Icons.Outlined.Description,
                            title = "License",
                            subtitle = "MIT License",
                            onClick = onOpenLicense
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = VidVibeTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Your videos stay on this device. VidVibe has no " +
                                "accounts, ads or tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VidVibeTextTertiary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "VidVibe ${BuildConfig.VERSION_NAME} · MIT License",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = VidVibeTextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileCard(
    videoCount: Int,
    playlistCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        color = VidVibeSurface2,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF12131A), RoundedCornerShape(16.dp))
                        .border(1.dp, VidVibeBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "VidVibe",
                        style = MaterialTheme.typography.titleLarge,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} · Open source",
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextTertiary
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileStat(
                    value = videoCount.toString(),
                    label = if (videoCount == 1) "video" else "videos",
                    modifier = Modifier.weight(1f)
                )
                ProfileStat(
                    value = playlistCount.toString(),
                    label = if (playlistCount == 1) "playlist" else "playlists",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF13141B), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = VidVibeTextTertiary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VidVibeTextSecondary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = VidVibeText,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VidVibeTextTertiary
            )
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun DownloadSettingsPage(
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val background = LocalAppBackground.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    onClick = onBack,
                    icon = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = VidVibeText
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "Download settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = VidVibeText,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Downloads are saved to Movies/VidVibe in your gallery. " +
                        "VidVibe removes its temporary copy as soon as the file is " +
                        "published.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VidVibeTextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VidVibeSurface2,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
                ) {
                    Column {
                        SettingRow(
                            icon = Icons.Outlined.Wifi,
                            title = "Wi-Fi only",
                            subtitle = "Prevent downloads on mobile data",
                            trailing = {
                                Switch(
                                    checked = wifiOnly,
                                    onCheckedChange = onWifiOnlyChange,
                                    colors = vidVibeSwitchColors()
                                )
                            }
                        )
                        HorizontalDivider(color = VidVibeBorder)
                        SettingRow(
                            icon = Icons.Outlined.Download,
                            title = "Default quality",
                            subtitle = "You choose quality for every download",
                            trailing = {
                                Text(
                                    text = "Ask",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VidVibeTextTertiary
                                )
                            }
                        )
                        HorizontalDivider(color = VidVibeBorder)
                        SettingRow(
                            icon = Icons.Outlined.VideoLibrary,
                            title = "Save location",
                            subtitle = "Movies/VidVibe",
                            trailing = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = VidVibeTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "VidVibe uses yt-dlp to resolve public links and selects a " +
                        "progressive MP4 stream. Video and audio are already combined, " +
                        "so no separate FFmpeg merge is required on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VidVibeTextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun vidVibeSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = VidVibeRed,
    uncheckedThumbColor = VidVibeTextSecondary,
    uncheckedTrackColor = VidVibeSurface3,
    uncheckedBorderColor = VidVibeBorder
)

private fun openUrl(context: Context, url: String, onError: (String) -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        onError("No browser app is available.")
    }
}
