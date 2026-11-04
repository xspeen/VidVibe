package com.vidvibe.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.R
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeSurface3
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import kotlinx.coroutines.delay

@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = VidVibeTextSecondary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun PermissionScreen(
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val background = LocalAppBackground.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(VidVibeSurface2)
                    .border(1.dp, VidVibeBorder, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(92.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "VidVibe",
                style = MaterialTheme.typography.headlineMedium,
                color = VidVibeText,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Access your videos",
                style = MaterialTheme.typography.titleMedium,
                color = VidVibeText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "VidVibe needs permission to read the videos on this device so it can show and play them. Your library never leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VidVibeRed,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Allow access", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open settings", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun VidVibeToastHost(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 88.dp
) {
    var lastMessage by remember { mutableStateOf("") }
    if (message != null) lastMessage = message

    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(260)) { it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            color = VidVibeSurface2,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
            shadowElevation = 10.dp
        ) {
            Text(
                text = lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
            )
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = VidVibeSurface2,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder)
    ) {
        Column(content = { content() })
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VidVibeText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = VidVibeText,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}
