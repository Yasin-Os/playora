package com.yasin.playora.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasin.playora.R
import com.yasin.playora.media.LocalMedia
import com.yasin.playora.ui.common.PermissionGate
import com.yasin.playora.ui.common.formatDuration
import kotlinx.coroutines.delay

@Composable
fun MusicListScreen(modifier: Modifier = Modifier) {
    PermissionGate {
        val viewModel: MusicViewModel = viewModel()
        val songs by viewModel.songs.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val current by viewModel.current.collectAsState()
        Column(modifier = modifier.fillMaxSize()) {
            if (isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (songs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_music))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == current?.id,
                            onClick = { viewModel.play(song) }
                        )
                    }
                }
            }
            if (current != null) {
                MiniPlayer(viewModel = viewModel, current = current!!)
            }
        }
    }
}

@Composable
private fun SongRow(song: LocalMedia, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniPlayer(viewModel: MusicViewModel, current: LocalMedia) {
    val player = viewModel.playerManager.exoPlayer
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = current.displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.pause else R.string.play
                    )
                )
            }
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
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
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
