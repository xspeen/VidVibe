package com.vidvibe.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JFileChooser

private val Bg = Color(0xFF08080B)
private val Panel = Color(0xFF101015)
private val Field = Color(0xFF0C0C11)
private val Glass = Color(0x14FFFFFF)
private val Stroke = Color(0x1AFFFFFF)
private val TextMain = Color(0xFFF3F3F6)
private val TextDim = Color(0xFF9AA0AA)
private val TextFaint = Color(0xFF5E626C)
private val Green = Color(0xFF30D158)
private val Blue = Color(0xFF4E9BFF)
private val Red = Color(0xFFFF5A5F)
private val Amber = Color(0xFFFFB020)

@Composable
fun App(state: AppState, onQuit: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(Quality.Best) }
    var libraryTick by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxSize()) {
            Sidebar(state, onQuit)
            Box(Modifier.width(1.dp).fillMaxHeight().background(Stroke))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                TopBar(state)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (state.page) {
                        Page.Download -> DownloadPage(
                            state = state,
                            url = url,
                            onUrlChange = { url = it },
                            quality = quality,
                            onQualityChange = { quality = it }
                        )

                        Page.Queue -> QueuePage(state)
                        Page.Library -> LibraryPage(
                            state = state,
                            tick = libraryTick,
                            onRefresh = { libraryTick++ }
                        )

                        Page.Settings -> SettingsPage(state)
                    }
                }
                StatusBar(state)
            }
        }
        Toast(state)
    }
}

@Composable
private fun Sidebar(state: AppState, onQuit: () -> Unit) {
    Column(
        Modifier
            .width(224.dp)
            .fillMaxHeight()
            .background(Panel)
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(38.dp)) {
                drawRoundRect(
                    color = Green,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(11f, 11f)
                )
                val triangle = Path().apply {
                    moveTo(size.width * 0.34f, size.height * 0.25f)
                    lineTo(size.width * 0.74f, size.height * 0.5f)
                    lineTo(size.width * 0.34f, size.height * 0.75f)
                    close()
                }
                drawPath(triangle, Color(0xFF08080B))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "VidVibe",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "Windows 2.8.3",
                    fontSize = 10.sp,
                    color = TextFaint
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Page.entries.forEach { page ->
            val selected = state.page == page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Glass else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) Color(0x2EFFFFFF) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { state.page = page }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Green else Color.Transparent)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = page.title,
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) TextMain else TextDim,
                    modifier = Modifier.weight(1f)
                )
                if (page == Page.Queue && state.activeCount > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Blue.copy(alpha = 0.22f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = state.activeCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        val setup = state.engine
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Glass)
                .border(1.dp, Stroke, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (setup?.ready == true) Green else Amber)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (setup?.ready == true) "Engine ready" else "Engine missing",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = setup?.message ?: "Checking...",
                fontSize = 9.sp,
                color = TextFaint,
                lineHeight = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Quit VidVibe",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onQuit)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun TopBar(state: AppState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.page.title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = state.page.subtitle,
                fontSize = 11.sp,
                color = TextDim
            )
        }
        val setup = state.engine
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Glass)
                .border(1.dp, Stroke, RoundedCornerShape(14.dp))
                .clickable { state.page = Page.Settings }
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (setup?.ready == true) Green else Amber)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = setup?.message ?: "Checking engine...",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
private fun StatusBar(state: AppState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Panel)
            .border(1.dp, Stroke)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (state.activeCount > 0) {
                "${state.activeCount} download${if (state.activeCount == 1) "" else "s"} active"
            } else {
                "Idle"
            },
            fontSize = 9.5.sp,
            color = TextFaint
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = state.settings.downloadDir.absolutePath,
            fontSize = 9.5.sp,
            color = TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DownloadPage(
    state: AppState,
    url: String,
    onUrlChange: (String) -> Unit,
    quality: Quality,
    onQualityChange: (Quality) -> Unit
) {
    val ready = state.engine?.ready == true
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                text = "PASTE A LINK",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextFaint,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UrlField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                GhostButton(
                    text = "Paste",
                    onClick = {
                        val clip = readClipboard()
                        if (clip.isNotBlank()) onUrlChange(clip.trim())
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "QUALITY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextFaint,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Quality.entries.forEach { option ->
                    Chip(
                        label = option.label,
                        selected = quality == option,
                        onClick = { onQualityChange(option) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = if (ready) "Download" else "Install the engine first",
                enabled = !state.installing,
                onClick = {
                    if (!ready) {
                        state.page = Page.Settings
                    } else if (state.enqueue(url, quality)) {
                        onUrlChange("")
                    }
                }
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = if (ready) {
                    "Downloads continue in the background. VidVibe keeps the queue alive when the window is hidden."
                } else {
                    "VidVibe ships with its own yt-dlp and ffmpeg. Open Settings to install or repair the engine."
                },
                fontSize = 9.5.sp,
                color = TextFaint,
                lineHeight = 14.sp
            )
        }

        Spacer(Modifier.height(26.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Recent downloads",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Open queue",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Blue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { state.page = Page.Queue }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        val recent = state.tasks.filter { it.status == TaskStatus.Complete }.takeLast(4).reversed()
        if (recent.isEmpty()) {
            EmptyState(
                title = "Nothing here yet",
                detail = "Your finished downloads will show up here with one-tap access."
            )
        } else {
            recent.forEach { task ->
                RecentRow(task, onOpen = { openTask(task) })
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun RecentRow(task: DownloadTask, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Glass)
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Green.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.file?.name ?: task.title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = task.file?.let {
                    "${formatSize(it.length())}  ${formatDate(it.lastModified())}"
                } ?: "Saved",
                fontSize = 9.5.sp,
                color = TextFaint
            )
        }
        GhostButton(text = "Open", onClick = onOpen)
    }
}

@Composable
private fun QueuePage(state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (state.tasks.isEmpty()) "Queue is empty" else "${state.tasks.size} in the list",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Text(
                    text = "Downloads run one at a time so every file keeps full speed.",
                    fontSize = 10.sp,
                    color = TextFaint
                )
            }
            GhostButton(
                text = "Clear finished",
                enabled = state.tasks.any { !it.active },
                onClick = { state.clearFinished() }
            )
        }

        Spacer(Modifier.height(14.dp))

        if (state.tasks.isEmpty()) {
            EmptyState(
                title = "Queue is clear",
                detail = "Paste a video link on the Download page and it will appear here instantly."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.tasks) { task ->
                    TaskCard(state, task)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TaskCard(state: AppState, task: DownloadTask) {
    val accent = when (task.status) {
        TaskStatus.Running -> Blue
        TaskStatus.Queued -> TextDim
        TaskStatus.Complete -> Green
        TaskStatus.Failed -> Red
        TaskStatus.Cancelled -> TextDim
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = task.error ?: task.detail,
                    fontSize = 10.sp,
                    color = if (task.status == TaskStatus.Failed) {
                        Red.copy(alpha = 0.9f)
                    } else {
                        TextDim
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (task.status) {
                        TaskStatus.Running -> "${(task.progress * 100).toInt()}%"
                        TaskStatus.Queued -> "Queued"
                        TaskStatus.Complete -> "Done"
                        TaskStatus.Failed -> "Failed"
                        TaskStatus.Cancelled -> "Cancelled"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }

        Spacer(Modifier.height(11.dp))
        ProgressBar(
            progress = if (task.status == TaskStatus.Complete) 1f else task.progress,
            color = accent,
            active = task.active
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (task.status) {
                TaskStatus.Running, TaskStatus.Queued -> {
                    GhostButton(text = "Cancel", onClick = { state.cancel(task) })
                }

                TaskStatus.Complete -> {
                    GhostButton(text = "Open", onClick = { openTask(task) })
                    GhostButton(
                        text = "Show in folder",
                        onClick = { task.file?.let { revealFile(it) } ?: openFolder(state.settings.downloadDir) }
                    )
                    GhostButton(text = "Remove", onClick = { state.remove(task) })
                }

                TaskStatus.Failed, TaskStatus.Cancelled -> {
                    GhostButton(text = "Retry", onClick = { state.retry(task) })
                    GhostButton(text = "Remove", onClick = { state.remove(task) })
                }
            }
        }
    }
}

@Composable
private fun LibraryPage(state: AppState, tick: Int, onRefresh: () -> Unit) {
    val completed = state.tasks.count { it.status == TaskStatus.Complete }
    val files = remember(tick, completed, state.settings.downloadDir) {
        scanLibrary(state.settings.downloadDir)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${files.size} file${if (files.size == 1) "" else "s"} in ${state.settings.downloadDir.name}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Text(
                    text = "Only media files are listed. Everything else in the folder is left alone.",
                    fontSize = 10.sp,
                    color = TextFaint
                )
            }
            GhostButton(text = "Refresh", onClick = onRefresh)
            Spacer(Modifier.width(8.dp))
            GhostButton(
                text = "Open folder",
                onClick = { openFolder(state.settings.downloadDir) }
            )
        }

        Spacer(Modifier.height(14.dp))

        if (files.isEmpty()) {
            EmptyState(
                title = "Library is empty",
                detail = "Finished downloads land in ${state.settings.downloadDir.absolutePath}."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    LibraryRow(file)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun LibraryRow(file: File) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Glass)
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Blue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = file.extension.uppercase().take(3),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Blue
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatSize(file.length())}  ${formatDate(file.lastModified())}",
                fontSize = 9.5.sp,
                color = TextFaint
            )
        }
        GhostButton(text = "Play", onClick = { openFile(file) })
        Spacer(Modifier.width(8.dp))
        GhostButton(text = "Show", onClick = { revealFile(file) })
    }
}

@Composable
private fun SettingsPage(state: AppState) {
    val setup = state.engine
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        SectionHeader("Storage", "Where finished files are written")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.settings.downloadDir.absolutePath,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMain,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Videos, audio and trimmed clips all go here.",
                        fontSize = 9.5.sp,
                        color = TextFaint
                    )
                }
                Spacer(Modifier.width(12.dp))
                GhostButton(
                    text = "Choose folder",
                    onClick = {
                        chooseFolder(state.settings.downloadDir)?.let { folder ->
                            state.settings.downloadDir = folder
                            state.settings.save()
                            state.showToast("Download folder updated.")
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                GhostButton(
                    text = "Open",
                    onClick = { openFolder(state.settings.downloadDir) }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionHeader("Cookies", "Automatic retry for private or age-gated videos")
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                text = "VidVibe never asks for a cookies.txt. Pick the browser you are signed in with; when a site asks for a login, the download is retried once with those cookies automatically.",
                fontSize = 10.sp,
                color = TextDim,
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COOKIE_OPTIONS.forEach { (key, label) ->
                    Chip(
                        label = label,
                        selected = state.settings.cookiesBrowser == key,
                        onClick = {
                            state.settings.cookiesBrowser = key
                            state.settings.save()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionHeader("Window", "How VidVibe behaves when you close it")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Close to tray",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain
                    )
                    Text(
                        text = "The window hides and the queue keeps running. Use the tray icon to come back.",
                        fontSize = 9.5.sp,
                        color = TextFaint,
                        lineHeight = 13.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Toggle(
                    checked = state.settings.closeToTray,
                    onChange = {
                        state.settings.closeToTray = it
                        state.settings.save()
                    }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionHeader("Download engine", "yt-dlp and ffmpeg bundled with VidVibe")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (setup?.ready == true) Green else Amber)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = setup?.message ?: "Checking...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            LabelValue("yt-dlp", setup?.ytDlp?.absolutePath ?: "Not found")
            Spacer(Modifier.height(5.dp))
            LabelValue("ffmpeg", setup?.ffmpeg?.absolutePath ?: "Not found")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    text = if (setup?.ready == true) "Repair engine" else "Install engine",
                    enabled = !state.installing,
                    onClick = { state.installEngine() }
                )
                GhostButton(
                    text = "Update yt-dlp",
                    enabled = !state.installing && setup?.ytDlp != null,
                    onClick = { state.updateEngine() }
                )
            }
            if (state.installing) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.installStatus,
                    fontSize = 9.5.sp,
                    color = TextDim
                )
                Spacer(Modifier.height(6.dp))
                ProgressBar(
                    progress = state.installProgress,
                    color = Blue,
                    active = true
                )
            } else if (state.installStatus.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.installStatus,
                    fontSize = 9.5.sp,
                    color = TextFaint
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionHeader("About", "VidVibe for Windows")
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                text = "VidVibe 2.8.3",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Downloads through yt-dlp with an ffmpeg merge step. Trimmed clips are stream-copied, so quality is never reduced. The Android build shares the same queue and cookie behaviour.",
                fontSize = 9.5.sp,
                color = TextFaint,
                lineHeight = 14.sp
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 2.dp, bottom = 9.dp)) {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain
        )
        Text(
            text = subtitle,
            fontSize = 9.5.sp,
            color = TextFaint
        )
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextFaint,
            modifier = Modifier.width(58.dp)
        )
        Text(
            text = value,
            fontSize = 9.5.sp,
            color = TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF15151B), Color(0xFF0E0E13))
                )
            )
            .border(1.dp, Stroke, RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .border(1.dp, Stroke, RoundedCornerShape(18.dp))
            .padding(vertical = 34.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = detail,
            fontSize = 10.sp,
            color = TextFaint,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun UrlField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Field)
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = "https://www.youtube.com/watch?v=...",
                fontSize = 12.sp,
                color = TextFaint
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TextMain, fontSize = 12.sp),
            cursorBrush = SolidColor(Green),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) Green else Color.White.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color(0xFF07110A) else TextFaint
        )
    }
}

@Composable
private fun GhostButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) TextMain.copy(alpha = 0.88f) else TextFaint
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.38f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) TextMain else TextDim
        )
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color, active: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.09f))
    ) {
        if (active && progress <= 0f) {
            Box(
                Modifier
                    .fillMaxWidth(0.32f)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) Green else Color.White.copy(alpha = 0.12f))
            .clickable { onChange(!checked) }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset(x = if (checked) 18.dp else 0.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun BoxScope.Toast(state: AppState) {
    val message = state.toast
    LaunchedEffect(message) {
        if (message != null) {
            delay(2800)
            state.toast = null
        }
    }
    if (message != null) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xF21A1A21))
                .border(1.dp, Stroke, RoundedCornerShape(13.dp))
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Text(
                text = message,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val COOKIE_OPTIONS = listOf(
    "none" to "None",
    "chrome" to "Chrome",
    "edge" to "Edge",
    "firefox" to "Firefox",
    "brave" to "Brave"
)

private fun readClipboard(): String = runCatching {
    val data = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor)
    data as? String ?: ""
}.getOrDefault("")

private fun chooseFolder(current: File): File? = runCatching {
    val chooser = JFileChooser(if (current.isDirectory) current else File(System.getProperty("user.home")))
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.dialogTitle = "Choose download folder"
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}.getOrNull()

private fun openTask(task: DownloadTask) {
    task.file?.let { openFile(it) }
}

private fun openFile(file: File) {
    runCatching { Desktop.getDesktop().open(file) }
}

private fun revealFile(file: File) {
    runCatching {
        ProcessBuilder("explorer.exe", "/select,${file.absolutePath}").start()
    }
}

private fun openFolder(directory: File) {
    runCatching {
        directory.mkdirs()
        Desktop.getDesktop().open(directory)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) {
        "${value.toInt()} ${units[index]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[index])
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(millis))
