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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yasin.playora.R
import com.yasin.playora.data.Playlist
import com.yasin.playora.media.LocalMedia
import com.yasin.playora.player.PlaybackViewModel
import com.yasin.playora.player.toLocalMedia
import com.yasin.playora.ui.common.PermissionGate
import com.yasin.playora.ui.common.formatDuration
import kotlinx.coroutines.launch

private enum class MusicTab { ALL, PLAYLISTS, FAVORITES }

@Composable
fun MusicListScreen(
    viewModel: PlaybackViewModel,
    onPlaylistClick: (Long) -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionGate {
        var tab by remember { mutableStateOf(MusicTab.ALL) }
        var showNewPlaylist by remember { mutableStateOf(false) }
        var addToPlaylistSong by remember { mutableStateOf<LocalMedia?>(null) }
        var showSleepTimer by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val songs by viewModel.songs.collectAsState()
        val playlists by viewModel.library.playlists.collectAsState(initial = emptyList())
        val favorites by viewModel.library.favorites.collectAsState(initial = emptyList())
        val queue by viewModel.queue.collectAsState()
        val currentIndex by viewModel.currentIndex.collectAsState()
        val favoriteUris = favorites.map { it.songUri }.toSet()

        Column(modifier = modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == MusicTab.ALL,
                    onClick = { tab = MusicTab.ALL },
                    text = { Text(stringResource(R.string.all_songs)) }
                )
                Tab(
                    selected = tab == MusicTab.PLAYLISTS,
                    onClick = { tab = MusicTab.PLAYLISTS },
                    text = { Text(stringResource(R.string.playlists)) }
                )
                Tab(
                    selected = tab == MusicTab.FAVORITES,
                    onClick = { tab = MusicTab.FAVORITES },
                    text = { Text(stringResource(R.string.favorites)) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    MusicTab.ALL -> SongList(
                        songs = songs,
                        currentUri = queue.getOrNull(currentIndex)?.mediaId,
                        favoriteUris = favoriteUris,
                        onSongClick = { index -> viewModel.playSongs(songs, index) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onAddToPlaylistClick = { addToPlaylistSong = it }
                    )
                    MusicTab.PLAYLISTS -> PlaylistList(
                        playlists = playlists,
                        viewModel = viewModel,
                        onNewPlaylistClick = { showNewPlaylist = true },
                        onPlaylistClick = onPlaylistClick
                    )
                    MusicTab.FAVORITES -> {
                        val favSongs = favorites.map { it.toLocalMedia() }
                        if (favSongs.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_favorites))
                            }
                        } else {
                            SongList(
                                songs = favSongs,
                                currentUri = queue.getOrNull(currentIndex)?.mediaId,
                                favoriteUris = favoriteUris,
                                onSongClick = { index -> viewModel.playSongs(favSongs, index) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onAddToPlaylistClick = { addToPlaylistSong = it }
                            )
                        }
                    }
                }
            }
            if (queue.isNotEmpty()) {
                MiniPlayer(
                    viewModel = viewModel,
                    onQueueClick = onQueueClick,
                    onSleepTimerClick = { showSleepTimer = true }
                )
            }
        }

        if (showNewPlaylist) {
            NewPlaylistDialog(
                onDismiss = { showNewPlaylist = false },
                onCreate = { name ->
                    scope.launch { viewModel.library.createPlaylist(name) }
                    showNewPlaylist = false
                }
            )
        }
        addToPlaylistSong?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onDismiss = { addToPlaylistSong = null },
                onPick = { playlistId ->
                    scope.launch { viewModel.library.addSongToPlaylist(playlistId, song) }
                    addToPlaylistSong = null
                }
            )
        }
        if (showSleepTimer) {
            SleepTimerDialog(
                onDismiss = { showSleepTimer = false },
                onPick = { minutes ->
                    viewModel.startSleepTimer(minutes)
                    showSleepTimer = false
                }
            )
        }
    }
}

@Composable
private fun SongList(
    songs: List<LocalMedia>,
    currentUri: String?,
    favoriteUris: Set<String>,
    onSongClick: (Int) -> Unit,
    onFavoriteClick: (LocalMedia) -> Unit,
    onAddToPlaylistClick: (LocalMedia) -> Unit
) {
    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_music))
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.uri }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.uri == currentUri,
                isFavorite = song.uri in favoriteUris,
                onClick = { onSongClick(index) },
                onFavoriteClick = { onFavoriteClick(song) },
                onAddToPlaylistClick = { onAddToPlaylistClick(song) }
            )
        }
    }
}

@Composable
private fun SongRow(
    song: LocalMedia,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.favorites),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAddToPlaylistClick) {
            Icon(
                imageVector = Icons.Filled.PlaylistAdd,
                contentDescription = stringResource(R.string.add_to_playlist)
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    viewModel: PlaybackViewModel,
    onNewPlaylistClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onNewPlaylistClick,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.new_playlist), modifier = Modifier.padding(start = 8.dp))
        }
        if (playlists.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_playlists))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists, key = { it.id }) { playlist ->
                    val songs by viewModel.library.playlistSongs(playlist.id)
                        .collectAsState(initial = emptyList())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(playlist.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${songs.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewPlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun AddToPlaylistDialog(
    song: LocalMedia,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.no_playlists))
            } else {
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(playlist.id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SleepTimerDialog(onDismiss: () -> Unit, onPick: (Int?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column {
                listOf(10, 20, 30, 60).forEach { minutes ->
                    Text(
                        text = "$minutes ${stringResource(R.string.minutes)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(minutes) }
                            .padding(vertical = 12.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.sleep_timer_off),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(null) }
                        .padding(vertical = 12.dp)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun MiniPlayer(
    viewModel: PlaybackViewModel,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val sleepRemaining by viewModel.sleepRemainingMs.collectAsState()
    val currentTitle = queue.getOrNull(currentIndex)?.mediaMetadata?.title?.toString() ?: ""
    var sliderPosition by remember { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(currentTitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (sleepRemaining != null) {
                    Text(
                        text = "${stringResource(R.string.sleep_timer)}: ${formatDuration(sleepRemaining!!)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { viewModel.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous))
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.pause else R.string.play
                    )
                )
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next))
            }
            IconButton(onClick = onQueueClick) {
                Icon(Icons.Filled.QueueMusic, contentDescription = stringResource(R.string.queue))
            }
            IconButton(onClick = onSleepTimerClick) {
                Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.sleep_timer))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            androidx.compose.material3.Slider(
                value = sliderPosition ?: position.toFloat(),
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    sliderPosition?.let { viewModel.seekTo(it.toLong()) }
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
