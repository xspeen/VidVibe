package com.vidvibe.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val state = remember { AppState() }
    var windowVisible by remember { mutableStateOf(true) }
    val icon = remember { appIconPainter() }

    Tray(
        icon = icon,
        state = rememberTrayState(),
        tooltip = "VidVibe",
        onAction = { windowVisible = true },
        menu = {
            Item("Open VidVibe", onClick = { windowVisible = true })
            Separator()
            Item(
                "Quit",
                onClick = {
                    state.quit()
                    exitApplication()
                }
            )
        }
    )

    Window(
        onCloseRequest = {
            if (state.settings.closeToTray || state.activeCount > 0) {
                windowVisible = false
            } else {
                state.quit()
                exitApplication()
            }
        },
        title = "VidVibe",
        state = rememberWindowState(
            width = 1180.dp,
            height = 800.dp,
            position = WindowPosition(Alignment.Center)
        ),
        visible = windowVisible,
        icon = icon
    ) {
        App(
            state = state,
            onQuit = {
                state.quit()
                exitApplication()
            }
        )
    }
}

private fun appIconPainter(): BitmapPainter {
    val image = ImageBitmap(64, 64)
    val canvas = GraphicsCanvas(image)
    val paint = Paint()
    paint.color = Color(0xFF0A0A0C)
    canvas.drawRect(Rect(0f, 0f, 64f, 64f), paint)
    paint.color = Color(0xFF30D158)
    canvas.drawCircle(Offset(32f, 32f), 25f, paint)
    paint.color = Color(0xFF08080B)
    val triangle = Path().apply {
        moveTo(26f, 19f)
        lineTo(47f, 32f)
        lineTo(26f, 45f)
        close()
    }
    canvas.drawPath(triangle, paint)
    return BitmapPainter(image)
}
