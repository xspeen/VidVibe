package com.vidvibe.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vidvibe.app.download.AudioEngine
import com.vidvibe.app.ui.theme.LocalAppBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The palette sitting behind the whole interface. [colors] come from the album
 * cover of whatever is playing so the UI is tinted by the music; when nothing
 * plays they fall back to the chosen background look.
 */
data class MeshSample(val colors: List<Color>)

private val FlatFallback = listOf(
    Color(0xFF2B3A55),
    Color(0xFF1E5F74),
    Color(0xFF2F4858),
    Color(0xFF3C2F4E),
    Color(0xFF16324F)
)

val LocalMeshEnabled = staticCompositionLocalOf { false }
val LocalMeshSample = staticCompositionLocalOf { MeshSample(FlatFallback) }

/** Anchors for the colour blobs, as a fraction of the canvas and of its width. */
private data class Anchor(val x: Float, val y: Float, val spread: Float)

private val anchors = listOf(
    Anchor(0.14f, 0.12f, 0.62f),
    Anchor(0.82f, 0.18f, 0.55f),
    Anchor(0.50f, 0.44f, 0.70f),
    Anchor(0.18f, 0.78f, 0.58f),
    Anchor(0.86f, 0.72f, 0.64f),
    Anchor(0.55f, 0.94f, 0.50f)
)

/**
 * Samples the album art down to a handful of colours. Everything is deliberately
 * quantised: the result is fed to radial gradients, so pixel-accurate colours
 * would only add noise while costing a decode on the main thread.
 */
@Composable
fun rememberAlbumMeshSample(uri: Uri?, fallback: Color): MeshSample {
    var sampled by remember(uri) { mutableStateOf<MeshSample?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uri, context) {
        if (uri == null) {
            sampled = null
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .memoryCacheKey("mesh-$uri")
            .build()
        val bitmap = runCatching {
            (context.imageLoader.execute(request) as? SuccessResult)
                ?.drawable
                ?.toBitmap()
        }.getOrNull()
        if (bitmap == null) {
            sampled = null
            return@LaunchedEffect
        }
        val colors = withContext(Dispatchers.IO) { sampleColors(bitmap) }
        sampled = if (colors.isEmpty()) null else MeshSample(colors)
    }

    return sampled ?: MeshSample(meshColorsFrom(fallback))
}

private fun sampleColors(bitmap: Bitmap): List<Color> {
    val small = runCatching {
        Bitmap.createScaledBitmap(bitmap, 72, 72, true)
    }.getOrElse { bitmap }
    val palette = runCatching { Palette.from(small).generate() }.getOrNull() ?: return emptyList()

    val swatches = listOfNotNull(
        palette.vibrantSwatch,
        palette.dominantSwatch,
        palette.mutedSwatch,
        palette.darkVibrantSwatch,
        palette.lightVibrantSwatch,
        palette.darkMutedSwatch,
        palette.lightMutedSwatch
    )

    // Bucket by coarse hue so near-identical swatches do not waste a blob.
    val seen = mutableSetOf<Int>()
    val colors = mutableListOf<Color>()
    for (swatch in swatches) {
        val bucket = (swatch.rgb and 0xF0F0F0)
        if (bucket in seen) continue
        seen += bucket
        colors += Color(swatch.rgb)
        if (colors.size == anchors.size) break
    }
    return colors
}

/** Derives a five-colour mesh from the flat background look. */
private fun meshColorsFrom(base: Color): List<Color> = listOf(
    base.copy(red = (base.red * 1.9f).coerceAtMost(1f), green = (base.green * 1.5f).coerceAtMost(1f)),
    base.copy(green = (base.green * 1.8f).coerceAtMost(1f), blue = (base.blue * 1.6f).coerceAtMost(1f)),
    base.copy(red = (base.red * 1.4f).coerceAtMost(1f), blue = (base.blue * 2.0f).coerceAtMost(1f)),
    base.copy(red = (base.red * 2.0f).coerceAtMost(1f), blue = (base.blue * 1.2f).coerceAtMost(1f)),
    base.copy(green = (base.green * 2.0f).coerceAtMost(1f), red = (base.red * 1.2f).coerceAtMost(1f))
).map { if (it == base) base.copy(alpha = 1f) else it }

private fun Drawable.toBitmap(): Bitmap? {
    if (this is BitmapDrawable) {
        bitmap?.let { return it }
    }
    val width = intrinsicWidth.takeIf { it > 0 } ?: return null
    val height = intrinsicHeight.takeIf { it > 0 } ?: return null
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    setBounds(0, 0, width, height)
    draw(canvas)
    return output
}

/**
 * The 2.8.5 surface: a flat dark-mode base with a heavily blurred, colour-sampled
 * layer of the album cover floating over it. Everything else in the app draws
 * frosted translucent panels on top of this.
 */
@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    val base = LocalAppBackground.current
    val enabled = LocalMeshEnabled.current
    val nowPlaying by AudioEngine.state.collectAsState()
    val sample = rememberAlbumMeshSample(
        uri = if (enabled) nowPlaying.current?.albumArtUri else null,
        fallback = base
    )

    if (!enabled) {
        Box(modifier.background(base))
        return
    }

    val transition = rememberInfiniteTransition(label = "mesh")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 96_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh-angle"
    )

    Box(modifier.background(base)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = angle
                    scaleX = 1.45f
                    scaleY = 1.45f
                }
                .blur(72.dp)
        ) {
            drawMesh(sample.colors)
        }

        // Readability scrim: the mesh stays a glow behind the panels rather
        // than competing with text.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.34f),
                        Color.Black.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.46f)
                    )
                )
            )
        }
    }
}

private fun DrawScope.drawMesh(colors: List<Color>) {
    if (colors.isEmpty()) return
    val radiusUnit = size.width.coerceAtLeast(size.height)
    anchors.forEachIndexed { index, anchor ->
        val color = colors[index % colors.size]
        val center = Offset(size.width * anchor.x, size.height * anchor.y)
        val radius = radiusUnit * anchor.spread
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.62f), color.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
    // A thin light sweep keeps the surface reading as glass, not paint.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.05f),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.5f, size.height * 0.05f),
        style = Stroke(width = 1f)
    )
}

/** Screen root background: mesh when enabled, the old wash when it is not. */
@Composable
fun ScreenBackdrop(modifier: Modifier = Modifier) {
    if (LocalMeshEnabled.current) {
        // The app-level mesh already sits behind every screen.
        Box(modifier.background(Color.Transparent))
    } else {
        FluidBackground(modifier)
    }
}

/** Root background for a screen: flat look normally, transparent over the mesh. */
@Composable
fun Modifier.screenSurface(): Modifier = screenSurface(LocalAppBackground.current)

@Composable
fun Modifier.screenSurface(color: Color): Modifier =
    if (LocalMeshEnabled.current) this else this.background(color)
