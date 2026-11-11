package com.vidvibe.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.LocalAppTextColor
import kotlinx.coroutines.delay

/**
 * First-run notification onboarding. A bell swings inside pulsing rings while a
 * mock notification card drops in and its progress bar breathes, so the user
 * sees exactly what VidVibe will show in the shade before granting access.
 */
@Composable
fun NotificationPermissionScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
    val background = LocalAppBackground.current
    val textColor = LocalAppTextColor.current
    val entrance = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 650,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NotificationPreview(
                textColor = textColor,
                modifier = Modifier.graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 36f
                }
            )

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Never lose a download",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "VidVibe keeps downloading in the background and shows live " +
                    "progress in your notification shade. You can leave the app " +
                    "while a video saves.",
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.62f),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Allow notifications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(6.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = "Not now",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.50f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "You can change this later in Profile > App Settings.",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.32f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NotificationPreview(
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val pulse = remember { Animatable(0f) }
    val swing = remember { Animatable(0f) }
    val progress = remember { Animatable(0.16f) }
    val cardDrop = remember { Animatable(-1f) }

    LaunchedEffect(Unit) {
        delay(240)
        cardDrop.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.66f, stiffness = 240f)
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1f, tween(1100, easing = LinearEasing))
            pulse.animateTo(0f, tween(1100, easing = LinearEasing))
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            swing.animateTo(9f, tween(260, easing = FastOutSlowInEasing))
            swing.animateTo(-9f, tween(520, easing = FastOutSlowInEasing))
            swing.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            delay(900)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            progress.animateTo(0.88f, tween(2200, easing = LinearEasing))
            progress.animateTo(0.16f, tween(900, easing = LinearEasing))
            delay(400)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(228.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(168.dp)) {
            val ring = size.minDimension / 2f
            drawCircle(
                color = Color(0xFF5AA9FF).copy(alpha = 0.10f + pulse.value * 0.10f),
                radius = ring * (0.62f + pulse.value * 0.30f)
            )
            drawCircle(
                color = Color(0xFF5AA9FF).copy(alpha = 0.20f),
                radius = ring * 0.56f,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        Canvas(
            modifier = Modifier
                .size(58.dp)
                .align(Alignment.TopCenter)
                .offset(y = 18.dp)
                .graphicsLayer {
                    rotationZ = swing.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.18f)
                }
        ) {
            val s = size.minDimension / 24f
            val stroke = Stroke(
                width = 2f * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            val bell = Path().apply {
                moveTo(5.5f * s, 17.5f * s)
                lineTo(18.5f * s, 17.5f * s)
                lineTo(17f * s, 14.5f * s)
                lineTo(17f * s, 10f * s)
                cubicTo(17f * s, 6.6f * s, 14.8f * s, 4f * s, 12f * s, 4f * s)
                cubicTo(9.2f * s, 4f * s, 7f * s, 6.6f * s, 7f * s, 10f * s)
                lineTo(7f * s, 14.5f * s)
                close()
            }
            drawPath(bell, color = textColor, style = stroke)
            drawLine(
                color = textColor,
                start = Offset(10f * s, 20f * s),
                end = Offset(14f * s, 20f * s),
                strokeWidth = 2f * s,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = textColor,
                radius = 1.5f * s,
                center = Offset(12f * s, 1.6f * s)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = cardDrop.value * 200f
                    alpha = 1f + cardDrop.value.coerceAtMost(0f)
                }
        ) {
            NotificationCard(
                progress = progress.value,
                accent = Color(0xFF5AA9FF)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    progress: Float,
    accent: Color
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1C1C24), Color(0xFF0E0E13))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(15.dp)) {
                val triangle = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.12f)
                    lineTo(size.width * 0.88f, size.height * 0.5f)
                    lineTo(size.width * 0.22f, size.height * 0.88f)
                    close()
                }
                drawPath(triangle, color = Color.Black)
            }
        }

        Spacer(Modifier.width(11.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VidVibe",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "now",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.40f)
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Downloading reel  •  ${(progress * 100f).toInt()}%",
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .background(accent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
