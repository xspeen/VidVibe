package com.vidvibe.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeRedSoft
import com.vidvibe.app.ui.theme.VidVibeTextSecondary

@Composable
fun SelectionMenu(
    visible: Boolean,
    onShare: () -> Unit,
    onAddTo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it + 40 },
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 430f)
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 430f)
        ) + fadeIn(tween(180)),
        exit = fadeOut(tween(150)) + slideOutVertically(
            targetOffsetY = { it + 40 },
            animationSpec = tween(220)
        ),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xFF22232F),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VidVibeBorder),
            shadowElevation = 18.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    onClick = onShare
                )
                SelectionButton(
                    icon = Icons.Outlined.PlaylistAdd,
                    label = "Add to",
                    onClick = onAddTo
                )
                SelectionButton(
                    icon = Icons.Outlined.DeleteOutline,
                    label = "Delete",
                    onClick = onDelete,
                    tint = VidVibeRedSoft
                )
            }
        }
    }
}

@Composable
private fun SelectionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = VidVibeTextSecondary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}
