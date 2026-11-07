package com.vidvibe.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavBlue = Color(0xFF4E9BFF)

private enum class NavGlyph { Local, Downloader, Trash, Me }

private data class NavDestination(
    val label: String,
    val glyph: NavGlyph
)

private val destinations = listOf(
    NavDestination("Local", NavGlyph.Local),
    NavDestination("Downloader", NavGlyph.Downloader),
    NavDestination("Trash", NavGlyph.Trash),
    NavDestination("Me", NavGlyph.Me)
)

@Composable
fun CustomBottomNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    liquidGlass: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        val shape = RoundedCornerShape(32.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(shape)
                .then(
                    if (liquidGlass) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(Color(0xB3141419), Color(0x99141419))
                            )
                        )
                    } else {
                        Modifier.background(Color(0xF208080A))
                    }
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = shape
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                NavItem(
                    destination = destination,
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val lift by animateFloatAsState(
        targetValue = if (selected) -2f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "nav-lift"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.5f,
        animationSpec = tween(300),
        label = "nav-alpha"
    )
    val liftPx = with(density) { lift.dp.toPx() }

    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NavGlyph(
            glyph = destination.glyph,
            selected = selected,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    translationY = liftPx
                    alpha = contentAlpha
                }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun NavGlyph(
    glyph: NavGlyph,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (selected) NavBlue else Color.White
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val w = 1.8f * s
        val stroke = Stroke(
            width = w,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        fun o(x: Float, y: Float) = Offset(x * s, y * s)

        when (glyph) {
            NavGlyph.Local -> {
                drawRoundRect(
                    color = color,
                    topLeft = o(3f, 5f),
                    size = Size(18f * s, 14f * s),
                    cornerRadius = CornerRadius(3f * s, 3f * s),
                    style = stroke
                )
                drawLine(color, o(3f, 10f), o(21f, 10f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(color, o(3f, 14f), o(21f, 14f), strokeWidth = w, cap = StrokeCap.Round)
                val triangle = Path().apply {
                    moveTo(10f * s, 9f * s)
                    lineTo(15f * s, 12f * s)
                    lineTo(10f * s, 15f * s)
                    close()
                }
                drawPath(triangle, color = NavBlue)
            }

            NavGlyph.Downloader -> {
                drawLine(color, o(12f, 3f), o(12f, 15f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(color, o(7f, 10f), o(12f, 15f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(color, o(12f, 15f), o(17f, 10f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(color, o(5f, 20f), o(19f, 20f), strokeWidth = w, cap = StrokeCap.Round)
                val tray = Path().apply {
                    moveTo(7f * s, 20f * s)
                    lineTo(7f * s, 18f * s)
                    quadraticBezierTo(7f * s, 16f * s, 9f * s, 16f * s)
                    lineTo(15f * s, 16f * s)
                    quadraticBezierTo(17f * s, 16f * s, 17f * s, 18f * s)
                    lineTo(17f * s, 20f * s)
                }
                drawPath(
                    path = tray,
                    color = color,
                    style = Stroke(
                        width = w,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2f * s, 4f * s),
                            0f
                        )
                    )
                )
            }

            NavGlyph.Trash -> {
                drawLine(color, o(3f, 6f), o(21f, 6f), strokeWidth = w, cap = StrokeCap.Round)
                val body = Path().apply {
                    moveTo(19f * s, 6f * s)
                    lineTo(19f * s, 20f * s)
                    quadraticBezierTo(19f * s, 22f * s, 17f * s, 22f * s)
                    lineTo(7f * s, 22f * s)
                    quadraticBezierTo(5f * s, 22f * s, 5f * s, 20f * s)
                    lineTo(5f * s, 6f * s)
                }
                drawPath(body, color = color, style = stroke)
                val lid = Path().apply {
                    moveTo(8f * s, 6f * s)
                    lineTo(8f * s, 4f * s)
                    quadraticBezierTo(8f * s, 2f * s, 10f * s, 2f * s)
                    lineTo(14f * s, 2f * s)
                    quadraticBezierTo(16f * s, 2f * s, 16f * s, 4f * s)
                    lineTo(16f * s, 6f * s)
                }
                drawPath(lid, color = color, style = stroke)
                drawLine(color, o(10f, 11f), o(10f, 17f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(color, o(14f, 11f), o(14f, 17f), strokeWidth = w, cap = StrokeCap.Round)
            }

            NavGlyph.Me -> {
                drawCircle(
                    color = color,
                    radius = 3.5f * s,
                    center = o(12f, 8f),
                    style = stroke
                )
                val body = Path().apply {
                    moveTo(5.5f * s, 20f * s)
                    cubicTo(
                        5.5f * s, 16.5f * s,
                        8f * s, 14.5f * s,
                        12f * s, 14.5f * s
                    )
                    cubicTo(
                        16f * s, 14.5f * s,
                        18.5f * s, 16.5f * s,
                        18.5f * s, 20f * s
                    )
                }
                drawPath(body, color = color, style = stroke)
            }
        }
    }
}
