package com.vidvibe.app.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vidvibe.app.BuildConfig
import com.vidvibe.app.R
import com.vidvibe.app.ui.theme.VidVibeAccentRed
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary

const val VIDVIBE_GITHUB_URL = "https://github.com/xspeen/VidVibe"

@Composable
fun AboutVidVibeDialog(
    onDismiss: () -> Unit,
    onOpenGitHub: () -> Unit
) {
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
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(VidVibeSurface2, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(76.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "VidVibe",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VidVibeText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VidVibeTextTertiary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "A fast, private video library and social video downloader. " +
                            "Your files stay on your device, and the whole app is free " +
                            "and open source.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VidVibeTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    AboutRow(
                        icon = Icons.Outlined.Code,
                        title = "Source code",
                        subtitle = "github.com/xspeen/VidVibe",
                        onClick = onOpenGitHub
                    )
                    Spacer(Modifier.height(8.dp))
                    AboutRow(
                        icon = Icons.Outlined.Description,
                        title = "License",
                        subtitle = "MIT License",
                        onClick = null
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Made with Kotlin, Jetpack Compose, Media3 and yt-dlp.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VidVibeTextTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            color = VidVibeAccentRed,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VidVibeSurface2, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VidVibeTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeText,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VidVibeTextTertiary
            )
        }
    }
}
