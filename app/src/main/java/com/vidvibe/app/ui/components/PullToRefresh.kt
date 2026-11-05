package com.vidvibe.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun PullToRefresh(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val maxPullPx = with(density) { 95.dp.toPx() }
    val refreshHoldPx = with(density) { 65.dp.toPx() }
    val thresholdPx = with(density) { 75.dp.toPx() }
    val spinnerHiddenPx = with(density) { 60.dp.toPx() }

    val scope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val refreshing = remember { mutableStateOf(false) }
    val currentOnRefresh = rememberUpdatedState(onRefresh)

    DisposableEffect(Unit) {
        onDispose {
            refreshing.value = false
        }
    }

    fun startRefresh() {
        if (refreshing.value) return
        refreshing.value = true
        scope.launch {
            try {
                pullOffset.animateTo(
                    refreshHoldPx,
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                )
                currentOnRefresh.value()
                // Hold the spinner for seven seconds, then always return the
                // list to its resting position even if the screen is left or
                // the scan is cancelled part-way through.
                delay(7000)
            } finally {
                withContext(NonCancellable) {
                    pullOffset.animateTo(
                        0f,
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    )
                }
                refreshing.value = false
            }
        }
    }

    val connection = remember(maxPullPx, refreshHoldPx, thresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullOffset.value > 0f) {
                    val consumedY = if (-available.y <= pullOffset.value) {
                        available.y
                    } else {
                        -pullOffset.value
                    }
                    val newValue = (pullOffset.value + consumedY).coerceAtLeast(0f)
                    scope.launch { pullOffset.snapTo(newValue) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f && !refreshing.value && pullOffset.value < maxPullPx) {
                    val newValue = (pullOffset.value + available.y * 0.45f)
                        .coerceIn(0f, maxPullPx)
                    scope.launch { pullOffset.snapTo(newValue) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(connection)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    waitForUpOrCancellation()
                    if (!refreshing.value && pullOffset.value > 0f) {
                        if (pullOffset.value > thresholdPx) {
                            startRefresh()
                        } else {
                            scope.launch {
                                pullOffset.animateTo(
                                    0f,
                                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = pullOffset.value }
        ) {
            content()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (pullOffset.value - spinnerHiddenPx).roundToInt()
                    )
                }
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            if (pullOffset.value > 0.5f || refreshing.value) {
                SpinnerBadge(size = 44.dp, bladeSize = 22.dp)
            }
        }
    }
}
