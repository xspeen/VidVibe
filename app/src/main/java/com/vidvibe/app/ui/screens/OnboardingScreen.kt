package com.vidvibe.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.ui.theme.VidVibeBorder
import com.vidvibe.app.ui.theme.VidVibeGreen
import com.vidvibe.app.ui.theme.VidVibeSelectionBlue
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val chips: List<ImageVector>
)

private val onboardingPages = listOf(
    OnboardingPage(
        eyebrow = "YOUR LIBRARY",
        title = "Every video, one calm place",
        subtitle = "Browse videos, folders, and playlists with a fast, private library that never leaves your phone.",
        icon = Icons.Outlined.VideoLibrary,
        accent = VidVibeSelectionBlue,
        chips = listOf(Icons.Outlined.PlayArrow, Icons.Outlined.VideoLibrary)
    ),
    OnboardingPage(
        eyebrow = "DOWNLOADER",
        title = "Save from any link",
        subtitle = "Paste a link from your favourite platform. VidVibe fetches the best quality and combines video and audio on your device.",
        icon = Icons.Outlined.Download,
        accent = VidVibeGreen,
        chips = listOf(Icons.Outlined.Download, Icons.Outlined.PlayArrow)
    ),
    OnboardingPage(
        eyebrow = "FOR CREATORS",
        title = "A studio in your pocket",
        subtitle = "Built for library people, music heads, studios, and artists who want their clips organised and ready to share.",
        icon = Icons.Outlined.GraphicEq,
        accent = Color(0xFFFFB74D),
        chips = listOf(Icons.Outlined.MusicNote, Icons.Outlined.GraphicEq)
    ),
    OnboardingPage(
        eyebrow = "PRIVATE BY DESIGN",
        title = "Only you get in",
        subtitle = "Turn on App Lock to protect your library with your fingerprint, face, or device screen lock. No accounts, no tracking.",
        icon = Icons.Outlined.Lock,
        accent = Color(0xFFE57373),
        chips = listOf(Icons.Outlined.Lock, Icons.Outlined.VideoLibrary)
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == onboardingPages.lastIndex

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050507), Color(0xFF0D0F16), Color(0xFF050507))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VidVibe",
                    style = MaterialTheme.typography.titleMedium,
                    color = VidVibeText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onFinish) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = VidVibeTextTertiary
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    pageIndex = page
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onboardingPages.indices.forEach { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (selected) 24.dp else 7.dp,
                                    height = 7.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    if (selected) onboardingPages[index].accent
                                    else VidVibeTextTertiary.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (lastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onboardingPages[pagerState.currentPage].accent,
                        contentColor = Color(0xFF050507)
                    )
                ) {
                    Text(
                        text = if (lastPage) "Get started" else "Continue",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (lastPage) {
                        "You can change App Lock and the welcome tour in Me."
                    } else {
                        "Swipe through the tour, or skip whenever you like."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = VidVibeTextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int
) {
    val float = rememberInfiniteTransition(label = "onboarding-float-$pageIndex")
    val drift by float.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200 + pageIndex * 180),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding-drift-$pageIndex"
    )
    val pulse by float.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding-pulse-$pageIndex"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(270.dp)
                .offset(y = drift.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accent.copy(alpha = 0.24f),
                                page.accent.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(38.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                page.accent.copy(alpha = 0.95f),
                                page.accent.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = Color(0xFF050507),
                    modifier = Modifier.size(58.dp)
                )
            }
            page.chips.forEachIndexed { index, chip ->
                val isFirst = index == 0
                Box(
                    modifier = Modifier
                        .offset(
                            x = (if (isFirst) -104.dp else 104.dp),
                            y = (if (isFirst) -58.dp else 62.dp) + (if (isFirst) drift.dp else -drift.dp)
                        )
                        .size(if (isFirst) 52.dp else 46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VidVibeSurface2)
                        .then(Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = chip,
                        contentDescription = null,
                        tint = page.accent,
                        modifier = Modifier.size(if (isFirst) 24.dp else 21.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .background(Color.Transparent, CircleShape)
                    .then(Modifier)
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = page.eyebrow,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = page.accent,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = VidVibeText,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 460.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = VidVibeTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 460.dp)
        )
    }
}
