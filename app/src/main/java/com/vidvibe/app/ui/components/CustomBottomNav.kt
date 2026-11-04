package com.vidvibe.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextTertiary

private data class NavDestination(
    val label: String,
    val icon: ImageVector
)

private val destinations = listOf(
    NavDestination("Local", Icons.Outlined.Home),
    NavDestination("Downloader", Icons.Outlined.Download),
    NavDestination("Me", Icons.Outlined.Person)
)

@Composable
fun CustomBottomNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    liquidGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val glassBrush = Brush.verticalGradient(
        listOf(
            Color(0xE61A1C24),
            Color(0xF20A0A0D)
        )
    )
    val solidBrush = SolidColor(Color(0xFF08080A))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = VidVibeBorder,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                if (liquidGlass) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.045f),
                        start = Offset(0f, 1.dp.toPx()),
                        end = Offset(size.width, 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            .background(if (liquidGlass) glassBrush else solidBrush)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            destinations.forEachIndexed { index, destination ->
                NavButton(
                    destination = destination,
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavButton(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 620f),
        label = "nav-indicator"
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(12.dp)
                .width(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            ScopedAnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(2.dp)
                        .background(
                            VidVibeText,
                            RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
                        )
                )
            }
        }

        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = if (selected) VidVibeText else VidVibeTextTertiary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) VidVibeText else VidVibeTextTertiary
        )
    }
}
