package com.vidvibe.app.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.formatFileSize
import com.vidvibe.app.ui.theme.AnimationStyle
import com.vidvibe.app.ui.theme.AppFont
import com.vidvibe.app.ui.theme.AppThemeMode
import com.vidvibe.app.ui.theme.BackgroundLook
import com.vidvibe.app.ui.theme.CustomizationSelection
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.MenuGlass
import com.vidvibe.app.ui.theme.TextTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val ProfileSurface = Color(0xFF1C1C21)
private val ProfileBorder = Color.White.copy(alpha = 0.08f)
private val ProfileText2 = Color(0xFFA0A0A8)
private val ProfileText3 = Color(0xFF6B6B75)
private val ProfileBlue = Color(0xFF4E9BFF)
private val ProfileGreen = Color(0xFF30D158)
private val ProfileGold = Color(0xFFFFD700)

private enum class ProfilePage {
    Home,
    History,
    Settings
}

@Composable
fun MeScreen(
    settings: AppSettings,
    librarySizeLabel: String,
    onApplyCustomization: (CustomizationSelection) -> Unit,
    onSubPageChanged: (Boolean) -> Unit,
    showToast: (String) -> Unit
) {
    var page by rememberSaveable { mutableStateOf(ProfilePage.Home) }

    LaunchedEffect(page) {
        onSubPageChanged(page != ProfilePage.Home)
    }

    DisposableEffect(Unit) {
        onDispose { onSubPageChanged(false) }
    }

    BackHandler(enabled = page != ProfilePage.Home) {
        page = ProfilePage.Home
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            if (forward) {
                (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 3 } + fadeOut(tween(150)))
            } else {
                (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(150)))
            }
        },
        label = "profile-page"
    ) { current ->
        when (current) {
            ProfilePage.Home -> ProfileHome(
                onOpenHistory = { page = ProfilePage.History },
                onOpenSettings = { page = ProfilePage.Settings }
            )

            ProfilePage.History -> DownloadHistoryPage(
                librarySizeLabel = librarySizeLabel,
                onBack = { page = ProfilePage.Home },
                onOpenSettings = { page = ProfilePage.Settings }
            )

            ProfilePage.Settings -> AppSettingsPage(
                settings = settings,
                onBack = { page = ProfilePage.Home },
                onApplyCustomization = onApplyCustomization,
                showToast = showToast
            )
        }
    }
}

@Composable
private fun ProfileHome(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val background = LocalAppBackground.current
    val textColor = LocalAppTextColor.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Text(
                text = "Profile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = textColor,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 20.dp)
            )

            SettingsGroup {
                SettingsRow(
                    icon = Icons.Outlined.Download,
                    title = "Download History",
                    onClick = onOpenHistory
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ProfileBorder)
                )
                SettingsRow(
                    icon = Icons.Outlined.Settings,
                    title = "App Settings",
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun DownloadHistoryPage(
    librarySizeLabel: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val background = LocalAppBackground.current
    val textColor = LocalAppTextColor.current
    val totalStorage = remember { deviceStorageTotal() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SubHeader(
                title = "Download History",
                onBack = onBack,
                actionIcon = Icons.Outlined.Settings,
                onAction = onOpenSettings
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No Downloads Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.70f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = librarySizeLabel,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF453A)
            )
            Text(
                text = " used of $totalStorage",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = ProfileText3
            )
        }
    }
}

@Composable
private fun AppSettingsPage(
    settings: AppSettings,
    onBack: () -> Unit,
    onApplyCustomization: (CustomizationSelection) -> Unit,
    showToast: (String) -> Unit
) {
    val background = LocalAppBackground.current

    var animation by remember { mutableStateOf(AnimationStyle.from(settings.animationStyle)) }
    var font by remember { mutableStateOf(AppFont.from(settings.appFont)) }
    var backgroundLook by remember {
        mutableStateOf(BackgroundLook.from(settings.backgroundLook))
    }
    var textTone by remember { mutableStateOf(TextTone.from(settings.textColor)) }
    var glass by remember { mutableStateOf(MenuGlass.from(settings.glassColor)) }
    var theme by remember { mutableStateOf(AppThemeMode.from(settings.appTheme)) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2600)
            showSuccess = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SubHeader(
                title = "App Settings",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp)
            ) {
                CustomGroup(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Animation Style"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimationStyle.entries.forEach { option ->
                            OptionButton(
                                label = option.label,
                                selected = animation == option,
                                onClick = { animation = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                CustomGroup(
                    icon = Icons.Outlined.TextFields,
                    title = "App Font"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFont.entries.forEach { option ->
                            OptionButton(
                                label = option.label,
                                selected = font == option,
                                onClick = { font = option },
                                fontFamily = option.family,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                CustomGroup(
                    icon = Icons.Outlined.Palette,
                    title = "Background Look"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackgroundLook.entries.forEach { option ->
                            OptionButton(
                                label = option.label,
                                selected = backgroundLook == option,
                                onClick = { backgroundLook = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                CustomGroup(
                    icon = Icons.Outlined.Palette,
                    title = "Text Color"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextTone.entries.forEach { option ->
                            TextColorDot(
                                color = option.color,
                                selected = textTone == option,
                                onClick = { textTone = option }
                            )
                        }
                    }
                }

                CustomGroup(
                    icon = Icons.Outlined.Settings,
                    title = "Floating Menu Colors"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MenuGlass.entries.forEach { option ->
                            GlassColorDot(
                                glass = option,
                                selected = glass == option,
                                onClick = { glass = option }
                            )
                        }
                    }
                }

                CustomGroup(
                    icon = Icons.Outlined.DarkMode,
                    title = "App Theme"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { option ->
                            OptionButton(
                                label = option.label,
                                selected = theme == option,
                                onClick = { theme = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                PremiumSlideToUpgrade(
                    onUpgrade = {
                        onApplyCustomization(
                            CustomizationSelection(
                                animationStyle = animation,
                                appFont = font,
                                backgroundLook = backgroundLook,
                                textColor = textTone,
                                glassColor = glass,
                                appTheme = theme
                            )
                        )
                        showToast("Customization applied")
                        showSuccess = true
                    }
                )
            }
        }

        if (showSuccess) {
            PremiumSuccessCircle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 124.dp)
            )
        }
    }
}

@Composable
private fun SubHeader(
    title: String,
    onBack: () -> Unit,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    val textColor = LocalAppTextColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundGlassIcon(
            icon = Icons.Outlined.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (actionIcon != null && onAction != null) {
            RoundGlassIcon(
                icon = actionIcon,
                contentDescription = "Settings",
                onClick = onAction
            )
        }
    }
}

@Composable
private fun RoundGlassIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ProfileSurface)
            .border(1.dp, ProfileBorder, RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.40f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CustomGroup(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ProfileSurface)
            .border(1.dp, ProfileBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ProfileText2,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileText2,
                letterSpacing = 0.65.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun OptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.SansSerif
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) ProfileBlue else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (selected) ProfileBlue else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontFamily,
            color = if (selected) Color.White else LocalAppTextColor.current
        )
    }
}

@Composable
private fun TextColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.20f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun GlassColorDot(
    glass: MenuGlass,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(glass.background)
            .border(
                width = 2.dp,
                color = if (selected) Color.White else glass.highlight,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun PremiumSlideToUpgrade(onUpgrade: () -> Unit) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val insetPx = with(density) { 4.dp.toPx() }
    val handlePx = with(density) { 48.dp.toPx() }
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val maxOffset = (trackWidthPx - handlePx - insetPx * 2f).coerceAtLeast(1f)

    val settle = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val current = if (dragging) dragValue else settle.value

    var upgraded by remember { mutableStateOf(false) }

    val pulse by rememberInfiniteTransition(label = "premium-pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premium-pulse-value"
    )
    val threshold = maxOffset * 0.7f
    val dragFade = if (upgraded) 0f else {
        (1f - current / threshold).coerceIn(0f, 1f)
    }
    val textAlpha = (0.4f + pulse * 0.4f) * dragFade

    LaunchedEffect(upgraded) {
        if (upgraded) {
            delay(300)
            onUpgrade()
            delay(2400)
            upgraded = false
            settle.snapTo(0f)
            dragValue = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 30.dp, bottom = 30.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x1AFFD700))
            .border(1.dp, Color(0x4DFFD700), RoundedCornerShape(32.dp))
            .onSizeChanged { trackWidthPx = it.width }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0x66000000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Slide to Upgrade",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = ProfileGold.copy(alpha = textAlpha),
                modifier = Modifier.alpha(textAlpha)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((current).roundToInt(), 0) }
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xE6FFD700), Color(0xE6FFA500))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                .pointerInput(maxOffset, upgraded) {
                    if (upgraded) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragValue = settle.value
                            haptics.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                            )
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragValue = (dragValue + dragAmount).coerceIn(0f, maxOffset)
                        },
                        onDragEnd = {
                            dragging = false
                            if (dragValue >= maxOffset * 0.7f) {
                                scope.launch {
                                    settle.snapTo(dragValue)
                                    settle.animateTo(
                                        targetValue = maxOffset,
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                                        )
                                    )
                                    upgraded = true
                                }
                            } else {
                                scope.launch {
                                    settle.snapTo(dragValue)
                                    settle.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                                        )
                                    )
                                    dragValue = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch {
                                settle.snapTo(dragValue)
                                settle.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                                    )
                                )
                                dragValue = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(24.dp)) {
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
                drawPath(crown, color = Color.Black)
            }
        }
    }
}

private data class PremiumSparkle(
    val angle: Float,
    val distance: Float,
    val color: Color
)

@Composable
private fun PremiumSuccessCircle(modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    val circle = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    val sparkles = remember {
        val colors = listOf(
            Color.White,
            ProfileGreen,
            Color(0xFFA7F3C5),
            ProfileGold
        )
        List(12) { index ->
            PremiumSparkle(
                angle = (index / 12f) * (2f * PI.toFloat()),
                distance = 40f + Random.nextFloat() * 30f,
                color = colors[index % colors.size]
            )
        }
    }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                )
            )
        }
        launch {
            delay(100)
            circle.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = CubicBezierEasing(0.65f, 0f, 0.45f, 1f)
                )
            )
        }
        launch {
            delay(800)
            check.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierCubicEasing()
                )
            )
        }
        launch {
            delay(700)
            burst.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }
    }

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        sparkles.forEach { sparkle ->
            val dx = cos(sparkle.angle) * sparkle.distance * burst.value
            val dy = sin(sparkle.angle) * sparkle.distance * burst.value
            Box(
                modifier = Modifier
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(6.dp)
                    .graphicsLayer {
                        val remaining = 1f - burst.value
                        scaleX = remaining
                        scaleY = remaining
                        rotationZ = 360f * burst.value
                        alpha = remaining
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(sparkle.color)
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(CircleShape)
                .background(ProfileGreen),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(32.dp)) {
                val s = size.width / 52f
                val center = Offset(26f * s, 26f * s)

                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 360f * circle.value,
                    useCenter = false,
                    topLeft = Offset(center.x - 25f * s, center.y - 25f * s),
                    size = Size(50f * s, 50f * s),
                    style = Stroke(width = 4f * s)
                )

                if (check.value > 0f) {
                    val path = Path().apply {
                        moveTo(14.1f * s, 27.2f * s)
                        lineTo(21.2f * s, 34.4f * s)
                        lineTo(37.9f * s, 17.6f * s)
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val segment = Path()
                    measure.getSegment(
                        startDistance = 0f,
                        stopDistance = measure.length * check.value,
                        destination = segment,
                        startWithMoveTo = true
                    )
                    drawPath(
                        path = segment,
                        color = Color.White,
                        style = Stroke(
                            width = 5f * s,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

private fun CubicBezierCubicEasing(): androidx.compose.animation.core.Easing =
    CubicBezierEasing(0.65f, 0f, 0.45f, 1f)

private fun deviceStorageTotal(): String {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        formatFileSize(stat.totalBytes)
    } catch (_: Exception) {
        "0 B"
    }
}
