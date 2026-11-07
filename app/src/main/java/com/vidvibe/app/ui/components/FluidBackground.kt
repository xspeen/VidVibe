package com.vidvibe.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vidvibe.app.ui.theme.LocalAppBackground

/**
 * The rotating conic wash plus the two radial glows used behind the
 * Local screen. Matches the prototype's `body` and `.fluid-bg`.
 */
@Composable
fun FluidBackground(modifier: Modifier = Modifier) {
    val base = LocalAppBackground.current

    if (base == Color.Black) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "fluid-background")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fluid-angle"
    )
    val tint = base.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = angle
                    scaleX = 1.8f
                    scaleY = 1.8f
                }
                .blur(40.dp)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            base,
                            tint,
                            base,
                            tint,
                            base
                        )
                    )
                )
        )

        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x260078FF), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.10f),
                    radius = size.width * 0.75f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1A00C896), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.90f),
                    radius = size.width * 0.75f
                )
            )
        }
    }
}
