package com.yasin.playora.player

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yasin.playora.data.Favorite
import com.yasin.playora.data.MusicLibraryRepository
import com.yasin.playora.data.PlaylistSong
import com.yasin.playora.media.LocalMedia
import com.yasin.playora.media.MediaStoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

fun LocalMedia.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setMediaId(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(displayName).build())
    .build()

fun PlaylistSong.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(songUri)
    .setMediaId(songUri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(songTitle).build())
    .build()

fun Favorite.toLocalMedia(): LocalMedia = LocalMedia(
    id = songUri.hashCode().toLong(),
    uri = songUri,
    displayName = songTitle,
    durationMs = durationMs,
    sizeBytes = 0L,
    dateAdded = 0L,
    mimeType = "audio/*",
    kind = com.yasin.playora.media.MediaKind.AUDIO
)

/**
 * Owns the [MediaController] connected to [PlaybackService]: the single
 * source of truth for audio playback, the queue and the sleep timer.
 * Scoped to the activity so every music screen shares one controller.
 */
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    val library = MusicLibraryRepository(application)
    private val mediaStore = MediaStoreRepository(application)

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _songs = MutableStateFlow<List<LocalMedia>>(emptyList())
    val songs: StateFlow<List<LocalMedia>> = _songs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs.asStateFlow()

    private var sleepJob: Job? = null
    private var sleepEndMs: Long? = null

    init {
        loadSongs()
        val token = SessionToken(
            application,
            ComponentName(application, PlaybackService::class.java)
        )
        val future = MediaController.Builder(application, token).buildAsync()
        future.addListener(
            { _controller.value = future.get() },
            ContextCompat.getMainExecutor(application)
        )
        viewModelScope.launch {
            while (true) {
                val c = _controller.value
                if (c != null) {
                    _isPlaying.value = c.isPlaying
                    _position.value = c.currentPosition
                    _duration.value = c.duration.coerceAtLeast(0L)
                    _currentIndex.value = c.currentMediaItemIndex
                    _queue.value = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
                }
                sleepEndMs?.let { end ->
                    val remaining = end - System.currentTimeMillis()
                    _sleepRemainingMs.value = if (remaining > 0) remaining else null
                }
                delay(500)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _songs.value = mediaStore.loadAudio()
        }
    }

    fun playSongs(songs: List<LocalMedia>, startIndex: Int = 0) {
        val c = _controller.value ?: return
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playPlaylistSongs(songs: List<PlaylistSong>, startIndex: Int = 0) {
        val c = _controller.value ?: return
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = _controller.value ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        _controller.value?.seekTo(positionMs)
    }

    fun seekToItem(index: Int) {
        _controller.value?.seekTo(index, 0L)
    }

    fun next() {
        _controller.value?.seekToNextMediaItem()
    }

    fun previous() {
        _controller.value?.seekToPreviousMediaItem()
    }

    fun toggleFavorite(song: LocalMedia) {
        viewModelScope.launch { library.toggleFavorite(song) }
    }

    /** Stops playback after [minutes]; pass null to cancel. */
    fun startSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            sleepEndMs = null
            _sleepRemainingMs.value = null
            return
        }
        sleepEndMs = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            _controller.value?.pause()
            sleepEndMs = null
            _sleepRemainingMs.value = null
        }
    }

    override fun onCleared() {
        _controller.value?.release()
        super.onCleared()
    }
}
