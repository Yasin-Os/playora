package com.yasin.playora.ui.video

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.yasin.playora.R
import com.yasin.playora.data.PlaybackHistoryStore
import com.yasin.playora.player.PlayerManager
import com.yasin.playora.ui.common.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {
    val playerManager = PlayerManager(application)
    private val history = PlaybackHistoryStore(application)

    fun play(uri: String) {
        playerManager.play(uri)
        val saved = history.getPosition(uri)
        if (saved > 0) playerManager.seekTo(saved)
    }

    fun savePosition(uri: String) {
        val pos = playerManager.exoPlayer.currentPosition
        if (pos > 0) history.savePosition(uri, pos)
    }

    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }
}

/**
 * Video player: PlayerView + custom Compose controls.
 * Double-tap left/right seeks ∓10s. Remembers the last position per video.
 */
@Composable
fun VideoPlayerScreen(uri: String, title: String) {
    val viewModel: VideoPlayerViewModel = viewModel()
    val context = LocalContext.current
    val activity = context as? Activity
    val player = viewModel.playerManager.exoPlayer

    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    LaunchedEffect(uri) { viewModel.play(uri) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePosition(uri)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let {
                WindowCompat.getInsetsController(it.window, it.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Poll playback state for the controls.
    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Fullscreen: landscape + hide system bars.
    LaunchedEffect(fullscreen) {
        activity?.let {
            it.requestedOrientation = if (fullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                if (fullscreen) hide(WindowInsetsCompat.Type.systemBars())
                else show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Double-tap left/right = seek -10s / +10s.
            // Vertical drag: left half = brightness, right half = volume.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val half = size.width / 2f
                                if (offset.x < half) {
                                    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                                } else {
                                    player.seekTo(
                                        (player.currentPosition + 10_000)
                                            .coerceAtMost(duration)
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var isLeft = true
                        var brightness = 0.5f
                        detectDragGestures(
                            onDragStart = { offset ->
                                isLeft = offset.x < size.width / 2f
                                brightness = activity?.window?.attributes?.screenBrightness
                                    ?.takeIf { it >= 0f } ?: 0.5f
                            },
                            onDrag = { _, dragAmount ->
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                if (isLeft) {
                                    brightness = (brightness - dragAmount.y / height)
                                        .coerceIn(0.05f, 1f)
                                    activity?.window?.let { window ->
                                        val attrs = window.attributes
                                        attrs.screenBrightness = brightness
                                        window.attributes = attrs
                                    }
                                    gestureHint = "Brightness ${(brightness * 100).toInt()}%"
                                } else {
                                    val max = audioManager
                                        .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val current = audioManager
                                        .getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val next = (current - dragAmount.y / height * max)
                                        .toInt().coerceIn(0, max)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC, next, 0
                                    )
                                    gestureHint = "Volume ${next * 100 / max.coerceAtLeast(1)}%"
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    delay(900)
                                    gestureHint = null
                                }
                            }
                        )
                    }
            ) {
                gestureHint?.let { hint ->
                    Text(
                        text = hint,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            if (!fullscreen) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
        }
        // Controls.
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDuration(position),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Slider(
                    value = sliderPosition ?: position.toFloat(),
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        sliderPosition?.let { player.seekTo(it.toLong()) }
                        sliderPosition = null
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.pause else R.string.play
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { fullscreen = !fullscreen }) {
                    Icon(
                        imageVector = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = stringResource(
                            if (fullscreen) R.string.exit_fullscreen else R.string.fullscreen
                        )
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
