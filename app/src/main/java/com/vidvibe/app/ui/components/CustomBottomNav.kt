package com.vidvibe.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    if (liquidGlass) {
        LiquidGlassNav(
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    } else {
        SolidNav(
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    }
}

@Composable
private fun SolidNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            }
            .background(SolidColor(Color(0xFF08080A)))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            destinations.forEachIndexed { index, destination ->
                NavCell(
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
private fun LiquidGlassNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dragThreshold = with(density) { 46.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val cellWidth = maxWidth / destinations.size.toFloat()
            val progress by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 360f
                ),
                label = "liquid-nav-progress"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xE6222530),
                                Color(0xF20A0B10)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .pointerInput(selectedIndex) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragTotal += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    dragTotal <= -dragThreshold ->
                                        onSelect((selectedIndex + 1).coerceAtMost(destinations.lastIndex))
                                    dragTotal >= dragThreshold ->
                                        onSelect((selectedIndex - 1).coerceAtLeast(0))
                                }
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f }
                        )
                    }
            ) {
                // Soft liquid glow that trails the selected cell.
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 9.dp, vertical = 11.dp)
                        .graphicsLayer {
                            translationX = progress * cellWidth.toPx()
                            alpha = 0.62f
                        }
                        .blur(18.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.36f),
                                    Color(0xFF82B1FF).copy(alpha = 0.28f)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                )

                // Glass capsule that glides between tabs.
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = progress * cellWidth.toPx()
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.045f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.34f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            shape = RoundedCornerShape(19.dp)
                        )
                )

                // Specular highlight along the top edge.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 20.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.32f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    destinations.forEachIndexed { index, destination ->
                        NavCell(
                            destination = destination,
                            selected = selectedIndex == index,
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavCell(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 0.94f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 620f),
        label = "nav-icon-scale"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.52f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "nav-label-alpha"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = if (selected) VidVibeText else VidVibeTextTertiary,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) VidVibeText else VidVibeTextTertiary,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
        )
    }
}
