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
 * Ultra-sleek app wash: one very slow conic drift, two low-intensity colour
 * orbs, a top light and a bottom vignette. Everything sits under the content
 * at single-digit alpha so text stays crisp and the glass surfaces above keep
 * their contrast. Pure black stays pure black.
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
            animation = tween(durationMillis = 64000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fluid-angle"
    )
    val tint = base.copy(alpha = 0.34f)

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
                    scaleX = 2.2f
                    scaleY = 2.2f
                }
                .blur(64.dp)
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
                    colors = listOf(Color(0x1A0078FF), Color.Transparent),
                    center = Offset(size.width * 0.08f, size.height * 0.06f),
                    radius = size.width * 0.82f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1200C896), Color.Transparent),
                    center = Offset(size.width * 0.94f, size.height * 0.92f),
                    radius = size.width * 0.82f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x10FFFFFF), Color.Transparent),
                    center = Offset(size.width * 0.5f, -size.height * 0.04f),
                    radius = size.width * 0.9f
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.28f)
                    )
                )
            )
        }
    }
}
