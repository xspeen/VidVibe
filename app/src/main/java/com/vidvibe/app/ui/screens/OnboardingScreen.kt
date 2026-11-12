package com.vidvibe.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class TourPage(
    val imageUrl: String,
    val eyebrow: String,
    val title: String,
    val titleItalic: String,
    val subtitle: String,
    val meta: String
)

private val tourPages = listOf(
    TourPage(
        imageUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1000&q=80&auto=format&fit=crop",
        eyebrow = "Your Library",
        title = "Every frame,",
        titleItalic = "in its place.",
        subtitle = "A private cinema that never leaves your device. Browse films, " +
            "reels and playlists with the speed of a cutting room.",
        meta = "4K · 24:16"
    ),
    TourPage(
        imageUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=1000&q=80&auto=format&fit=crop",
        eyebrow = "Downloader",
        title = "Save from",
        titleItalic = "any link.",
        subtitle = "Paste a URL. VidVibe fetches the highest bitrate and muxes " +
            "video and audio locally — no cloud, no compromise.",
        meta = "HDR · 4K60"
    ),
    TourPage(
        imageUrl = "https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?w=1000&q=80&auto=format&fit=crop",
        eyebrow = "For Creators",
        title = "A studio in",
        titleItalic = "your pocket.",
        subtitle = "Built for library people, music heads and production teams who " +
            "need their clips organised and ready to share in seconds.",
        meta = ""
    ),
    TourPage(
        imageUrl = "https://images.unsplash.com/photo-1497032628192-86f99bcd76bc?w=1000&q=80&auto=format&fit=crop",
        eyebrow = "Private by design",
        title = "Only you",
        titleItalic = "get in.",
        subtitle = "No accounts, no tracking, no analytics. " +
            "Your work stays yours.",
        meta = ""
    )
)

private val TourGreen = Color(0xFF30D158)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tourPages.size })
    val scope = rememberCoroutineScope()

    var accepted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var shake by remember { mutableStateOf(false) }

    val shakeOffset by animateFloatAsState(
        targetValue = if (shake) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 420
            0f at 0
            10f at 55
            -10f at 110
            9f at 165
            -9f at 220
            5f at 275
            0f at 330
        },
        label = "tour-shake"
    )

    LaunchedEffect(shake) {
        if (shake) {
            delay(460)
            shake = false
        }
    }

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onFinish()
        }
    }

    val lastPage = pagerState.currentPage == tourPages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = tourPages[page].imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.96f)
                                )
                            )
                        )
                )
            }
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.86f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 20.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = "VidVibe",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onFinish, enabled = !loading) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = {
                        fadeIn(tween(480)) togetherWith fadeOut(tween(420))
                    },
                    label = "tour-text"
                ) { targetPage ->
                    val page = tourPages[targetPage]
                    Column {
                        if (page.meta.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.32f))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.22f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = page.meta,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        Text(
                            text = page.eyebrow.uppercase(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = page.title,
                            color = Color.White,
                            fontSize = 40.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 44.sp
                        )
                        Text(
                            text = page.titleItalic,
                            color = Color.White,
                            fontSize = 40.sp,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 44.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = page.subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            AnimatedVisibility(
                visible = !loading,
                enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 2 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { accepted = !accepted }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (accepted) TourGreen.copy(alpha = 0.16f)
                                    else Color.Black.copy(alpha = 0.32f)
                                )
                                .border(
                                    1.5.dp,
                                    if (accepted) TourGreen else Color.White.copy(alpha = 0.5f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(14.dp)) {
                                if (accepted) {
                                    val path = Path().apply {
                                        moveTo(size.width * 0.1f, size.height * 0.5f)
                                        lineTo(size.width * 0.4f, size.height * 0.8f)
                                        lineTo(size.width * 0.9f, size.height * 0.2f)
                                    }
                                    drawPath(
                                        path = path,
                                        color = TourGreen,
                                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "By continuing you agree to our Terms & Privacy Policy",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TourButton(
                            accepted = accepted,
                            loading = loading,
                            shakeOffset = shakeOffset,
                            label = if (lastPage) "Get started" else "Continue",
                            modifier = Modifier
                                .fillMaxWidth(if (loading) 0.16f else 1f)
                                .fillMaxHeight()
                                .clickable(enabled = !loading) {
                                    when {
                                        !accepted -> shake = true
                                        lastPage -> {
                                            loading = true
                                            scope.launch {
                                                delay(2200)
                                                onFinish()
                                                loading = false
                                            }
                                        }
                                        else -> scope.launch {
                                            pagerState.animateScrollToPage(
                                                pagerState.currentPage + 1
                                            )
                                        }
                                    }
                                }
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (lastPage) "Get started" else "Continue",
                                        color = if (accepted) Color.Black
                                        else Color.White.copy(alpha = 0.34f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (accepted) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        tourPages.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(4.dp)
                                    .width(if (pagerState.currentPage == index) 22.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) Color.White
                                        else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TourButton(
    accepted: Boolean,
    loading: Boolean,
    shakeOffset: Float,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (loading) 50.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tour-button-corner"
    )
    val background by animateColorAsState(
        targetValue = if (accepted || loading) Color.White else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(280),
        label = "tour-button-color"
    )

    Box(
        modifier = modifier
            .offset(x = shakeOffset.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .then(
                if (accepted || loading) Modifier
                else Modifier.border(
                    1.dp,
                    Color.White.copy(alpha = 0.14f),
                    RoundedCornerShape(cornerRadius)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
