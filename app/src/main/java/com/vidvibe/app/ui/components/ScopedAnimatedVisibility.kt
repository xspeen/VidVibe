package com.vidvibe.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Calls the unscoped [AnimatedVisibility] overload.
 *
 * Compose 1.7 ships extra AnimatedVisibility extensions for ColumnScope and RowScope.
 * When the call site sits inside a nested Box, the outer scope receiver is hidden by
 * the layout scope marker, so Kotlin selects the scoped extension and fails with
 * "cannot be called in this context with an implicit receiver". Routing through this
 * function keeps the call site free of layout receivers.
 */
@Composable
fun ScopedAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}
