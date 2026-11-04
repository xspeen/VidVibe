package com.vidvibe.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vidvibe.app.ui.theme.VidVibeBorder

@Composable
fun ClassicLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    bladeCount: Int = 12
) {
    val transition = rememberInfiniteTransition(label = "classic-loader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "classic-loader-phase"
    )

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.48f
        val innerRadius = size.minDimension * 0.18f
        val strokeWidth = size.minDimension * 0.14f

        repeat(bladeCount) { index ->
            val trail = (phase - index.toFloat() / bladeCount + 1f) % 1f
            val alpha = 0.12f + 0.88f * (1f - trail)
            rotate(degrees = index * (360f / bladeCount), pivot = center) {
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y - innerRadius),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun SpinnerBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    bladeSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF1E2029), CircleShape)
            .border(1.dp, VidVibeBorderStrong, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ClassicLoader(modifier = Modifier.size(bladeSize))
    }
}

private val VidVibeBorderStrong = Color(0x59FFFFFF)
