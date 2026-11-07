package com.vidvibe.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
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
import com.vidvibe.app.ui.theme.LocalAppTextColor

private enum class NavGlyph { Local, Downloader, Trash, Profile }

private data class NavDestination(
    val label: String,
    val glyph: NavGlyph
)

private val destinations = listOf(
    NavDestination("Local", NavGlyph.Local),
    NavDestination("Downloader", NavGlyph.Downloader),
    NavDestination("Trash", NavGlyph.Trash),
    NavDestination("Profile", NavGlyph.Profile)
)

private val Gold = Color(0xFFFFD700)

@Composable
fun CustomBottomNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    liquidGlass: Boolean,
    visible: Boolean = true,
    hideFraction: Float = 2f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hiddenOffsetPx = with(density) { 76.dp.toPx() } * hideFraction
    val translation by animateFloatAsState(
        targetValue = if (visible) 0f else hiddenOffsetPx,
        animationSpec = tween(
            durationMillis = 400,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "nav-translation"
    )
    val textColor = LocalAppTextColor.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = translation }
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        val shape = RoundedCornerShape(38.dp)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(shape)
                .then(
                    if (liquidGlass) {
                        Modifier.background(Color(0xB30A0A0C))
                    } else {
                        Modifier.background(Color(0xF208080A))
                    }
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = shape
                )
                .padding(horizontal = 8.dp)
        ) {
            val indicatorWidth = 64.dp
            // Row uses SpaceAround with fixed 64dp items, so mirror that spacing.
            val gap = (maxWidth - indicatorWidth * destinations.size) / 8f
            val indicatorOffset by animateDpAsState(
                targetValue = gap + (indicatorWidth + gap * 2f) * selectedIndex.toFloat(),
                animationSpec = tween(
                    durationMillis = 450,
                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                ),
                label = "nav-indicator"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = indicatorOffset, y = 6.dp)
                    .width(indicatorWidth)
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.60f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEachIndexed { index, destination ->
                    NavItem(
                        destination = destination,
                        selected = selectedIndex == index,
                        enabled = visible,
                        textColor = textColor,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: NavDestination,
    selected: Boolean,
    enabled: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val lift by animateFloatAsState(
        targetValue = if (selected) -1f else 0f,
        animationSpec = tween(300, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
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
            .graphicsLayer { alpha = contentAlpha }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { translationY = liftPx },
            contentAlignment = Alignment.Center
        ) {
            NavGlyph(
                glyph = destination.glyph,
                color = textColor,
                modifier = Modifier.size(22.dp)
            )
            if (destination.glyph == NavGlyph.Profile) {
                Crown(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = textColor
        )
    }
}

@Composable
private fun Crown(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (selected) 0f else -10f,
        animationSpec = tween(300, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label = "crown-rotation"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = tween(300, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label = "crown-scale"
    )
    Canvas(
        modifier = modifier
            .size(14.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
    ) {
        val s = size.minDimension / 24f
        val crown = Path().apply {
            moveTo(2f * s, 19f * s)
            lineTo(22f * s, 19f * s)
            lineTo(22f * s, 21f * s)
            lineTo(2f * s, 21f * s)
            close()
            moveTo(2f * s, 9f * s)
            lineTo(6f * s, 13f * s)
            lineTo(12f * s, 5f * s)
            lineTo(18f * s, 13f * s)
            lineTo(22f * s, 9f * s)
            lineTo(20f * s, 19f * s)
            lineTo(4f * s, 19f * s)
            close()
        }
        drawPath(crown, color = Gold)
    }
}

@Composable
private fun NavGlyph(
    glyph: NavGlyph,
    color: Color,
    modifier: Modifier = Modifier
) {
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
                drawPath(triangle, color = color)
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

            NavGlyph.Profile -> {
                drawCircle(
                    color = color,
                    radius = 4f * s,
                    center = o(12f, 7f)
                )
                val body = Path().apply {
                    moveTo(4f * s, 21f * s)
                    lineTo(4f * s, 20f * s)
                    cubicTo(
                        4f * s, 16.5f * s,
                        7.5f * s, 14f * s,
                        12f * s, 14f * s
                    )
                    cubicTo(
                        16.5f * s, 14f * s,
                        20f * s, 16.5f * s,
                        20f * s, 20f * s
                    )
                    lineTo(20f * s, 21f * s)
                    close()
                }
                drawPath(body, color = color)
            }
        }
    }
}
