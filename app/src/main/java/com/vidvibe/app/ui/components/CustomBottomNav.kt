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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
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
import com.vidvibe.app.ui.theme.IconPack
import com.vidvibe.app.ui.theme.LocalGlassStyle
import com.vidvibe.app.ui.theme.LocalIconPack
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
    val glass = LocalGlassStyle.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = translation }
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        // A soft glass slab with a single light edge. The liquid setting only
        // changes how much light it carries, never the geometry.
        val shape = RoundedCornerShape(20.dp)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = glass.glow,
                    spotColor = glass.glow
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(listOf(glass.bodyTop, glass.bodyBottom))
                )
                .border(width = 1.dp, color = glass.stroke, shape = shape)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(glass.sheen)
            )

            val indicatorWidth = 60.dp
            val gap = (maxWidth - indicatorWidth * destinations.size) / 8f
            val indicatorOffset by animateDpAsState(
                targetValue = gap + (indicatorWidth + gap * 2f) * selectedIndex.toFloat(),
                animationSpec = tween(
                    durationMillis = 380,
                    easing = CubicBezierEasing(0.2f, 1f, 0.3f, 1f)
                ),
                label = "nav-indicator"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = indicatorOffset, y = 7.dp)
                    .width(indicatorWidth)
                    .height(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(glass.indicator)
                    .border(
                        width = 1.dp,
                        color = glass.indicatorStroke,
                        shape = RoundedCornerShape(15.dp)
                    )
            )

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
        targetValue = if (selected) 1f else 0.58f,
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
                .size(20.dp)
                .graphicsLayer { translationY = liftPx },
            contentAlignment = Alignment.Center
        ) {
            NavGlyph(
                glyph = destination.glyph,
                color = textColor,
                modifier = Modifier.size(20.dp)
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
        Spacer(Modifier.height(5.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
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
    val pack = LocalIconPack.current
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val strokeWidth = when (pack) {
            IconPack.Line -> 1.7f
            IconPack.Rounded -> 2.1f
            IconPack.Bold -> 2.7f
            IconPack.Duotone -> 1.9f
        } * s
        fun o(x: Float, y: Float) = Offset(x * s, y * s)

        fun drawGlyph(color: Color, w: Float) {
        val stroke = Stroke(
            width = w,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

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

        if (pack == IconPack.Duotone) {
            drawGlyph(color.copy(alpha = 0.20f), strokeWidth * 3.6f)
        }
        drawGlyph(color, strokeWidth)
    }
}

/**
 * Landscape / wide-screen navigation rail. It mirrors the bottom bar exactly
 * (same glyphs, glass body and selection language) but stands vertically on
 * the leading edge so the content never has to scroll just to reach a tab.
 * Width animates to zero when a full-screen overlay takes over, which lets the
 * content reclaim the whole display without a layout jump.
 */
@Composable
fun CustomSideNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    liquidGlass: Boolean,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textColor = LocalAppTextColor.current
    val glass = LocalGlassStyle.current
    val railWidth by animateDpAsState(
        targetValue = if (visible) 92.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 320,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "side-nav-width"
    )

    Box(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(listOf(glass.bodyTop, glass.bodyBottom))
            )
            .border(
                width = 1.dp,
                color = glass.stroke,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        // Fixed-width content keeps the rail from reflowing while it collapses.
        Column(
            modifier = Modifier
                .width(92.dp)
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SideBrandMark()
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Spacer(Modifier.weight(1f))
            destinations.forEachIndexed { index, destination ->
                SideNavItem(
                    destination = destination,
                    selected = selectedIndex == index,
                    textColor = textColor,
                    onClick = { onSelect(index) }
                )
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "VIBE",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = textColor.copy(alpha = 0.24f)
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SideBrandMark() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(16.dp)) {
            val triangle = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.14f)
                lineTo(size.width * 0.86f, size.height * 0.5f)
                lineTo(size.width * 0.24f, size.height * 0.86f)
                close()
            }
            drawPath(triangle, color = Color.Black)
        }
    }
}

@Composable
private fun SideNavItem(
    destination: NavDestination,
    selected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(280, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label = "side-nav-scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.58f,
        animationSpec = tween(280),
        label = "side-nav-alpha"
    )
    val shape = RoundedCornerShape(16.dp)
    val glass = LocalGlassStyle.current

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = contentAlpha
            }
            .clip(shape)
            .background(
                if (selected) glass.indicator else Color.Transparent
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = glass.indicatorStroke,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                NavGlyph(
                    glyph = destination.glyph,
                    color = textColor,
                    modifier = Modifier.size(20.dp)
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
            Spacer(Modifier.height(5.dp))
            Text(
                text = destination.label,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                color = textColor
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .width(2.5.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Gold)
            )
        }
    }
}
