package com.vidvibe.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vidvibe.app.data.VideoItem

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBack by rememberUpdatedState(onBack)

    val exoPlayer = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler { currentOnBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { playerContext ->
                PlayerView(playerContext).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setKeepContentOnPlayerReset(true)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .clickable(onClick = { currentOnBack() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = video.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .clickable { shareVideo(context, video) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share video",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

private fun shareVideo(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, video.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri("video", video.uri)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share video"))
    }
}
